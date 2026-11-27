package com.salesanalytics.controller;

import com.salesanalytics.dto.ApiResponse;
import com.salesanalytics.entity.AppUser;
import com.salesanalytics.entity.ChatMessage;
import com.salesanalytics.repository.AppUserRepository;
import com.salesanalytics.repository.ChatMessageRepository;
import com.salesanalytics.service.RealtimeEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Direct Messaging Microservice Module (/api/chat/**).
 * Supports private 1-on-1 Buyer <-> Seller chats (only visible to participants + Admin oversight)
 * as well as direct User <-> Admin support threads.
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatMessageRepository chatRepo;
    private final AppUserRepository userRepo;
    private final RealtimeEventService realtimeEventService;

    public ChatController(ChatMessageRepository chatRepo,
                          AppUserRepository userRepo,
                          RealtimeEventService realtimeEventService) {
        this.chatRepo = chatRepo;
        this.userRepo = userRepo;
        this.realtimeEventService = realtimeEventService;
    }

    // ── SEND MESSAGE (Private 1-on-1 DM to Seller/Buyer OR Admin Support) ─────
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendMessage(
            @RequestBody Map<String, Object> req,
            Authentication auth) {

        String senderEmail = auth.getName();
        AppUser sender = userRepo.findByEmailIgnoreCase(senderEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String messageText = (String) req.getOrDefault("message", "");
        if (messageText == null || messageText.trim().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Message cannot be empty."));
        }

        String messageType = (String) req.getOrDefault("messageType", "TEXT");
        Object productIdObj = req.get("productId");
        String productName = (String) req.getOrDefault("productName", "");
        String recipientEmail = (String) req.getOrDefault("recipientEmail", "");
        if (recipientEmail != null) recipientEmail = recipientEmail.trim();
        if ("ADMIN_SUPPORT".equalsIgnoreCase(recipientEmail)) {
            recipientEmail = "";
        }

        ChatMessage msg = new ChatMessage();
        msg.setSenderEmail(senderEmail);
        msg.setSenderName(sender.getFullName());
        msg.setSenderRole(sender.getRole());
        msg.setMessage(messageText.trim());
        msg.setMessageType(messageType);
        if (productIdObj != null && !String.valueOf(productIdObj).isBlank()) {
            msg.setProductId(((Number) productIdObj).longValue());
            msg.setProductName(productName);
        }
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            msg.setRecipientEmail(recipientEmail);
        }
        chatRepo.save(msg);

        realtimeEventService.publish("CHAT_MESSAGE",
                "New message from " + sender.getFullName() + ": " + (messageText.length() > 60 ? messageText.substring(0, 60) + "…" : messageText),
                sender.getFullName());

        return ResponseEntity.ok(ApiResponse.ok("Message sent", toMap(msg)));
    }

    // ── USER: Get list of my private 1-on-1 conversations ─────────────────────
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyConversations(Authentication auth) {
        String myEmail = auth.getName().toLowerCase();
        List<ChatMessage> all = chatRepo.findAllForUser(myEmail);

        Map<String, Map<String, Object>> convos = new LinkedHashMap<>();
        // Always include Admin Support channel
        Map<String, Object> adminSlot = new HashMap<>();
        adminSlot.put("peerEmail", "ADMIN_SUPPORT");
        adminSlot.put("peerName", "Admin & Platform Support");
        adminSlot.put("isAdminSupport", true);
        adminSlot.put("latestMessage", "Ask admin about orders, approvals, or bulk deals");
        adminSlot.put("latestTime", "");
        adminSlot.put("unreadCount", 0L);
        adminSlot.put("productId", "");
        adminSlot.put("productName", "");
        convos.put("ADMIN_SUPPORT", adminSlot);

        for (ChatMessage m : all) {
            boolean isSupport = (m.getRecipientEmail() == null || m.getRecipientEmail().isBlank())
                    || "ADMIN".equalsIgnoreCase(m.getSenderRole());
            String peerKey;
            String peerName;

            if (isSupport && (m.getRecipientEmail() == null || m.getRecipientEmail().isBlank() || m.getSenderRole().equalsIgnoreCase("ADMIN"))) {
                // Support thread with Admin
                peerKey = "ADMIN_SUPPORT";
                peerName = "Admin & Platform Support";
                Map<String, Object> existing = convos.get(peerKey);
                if (existing.get("latestTime").toString().isEmpty()) {
                    existing.put("latestMessage", m.getMessage());
                    existing.put("latestTime", m.getCreatedAt().toString());
                }
                if (!m.isRead() && "ADMIN".equalsIgnoreCase(m.getSenderRole()) && m.getRecipientEmail() != null && m.getRecipientEmail().equalsIgnoreCase(myEmail)) {
                    existing.put("unreadCount", ((Long) existing.get("unreadCount")) + 1L);
                }
                continue;
            }

            if (m.getSenderEmail().equalsIgnoreCase(myEmail)) {
                peerKey = m.getRecipientEmail().toLowerCase();
                AppUser peer = userRepo.findByEmailIgnoreCase(peerKey).orElse(null);
                peerName = peer != null ? peer.getFullName() : peerKey;
            } else {
                peerKey = m.getSenderEmail().toLowerCase();
                peerName = m.getSenderName();
            }

            Map<String, Object> slot = convos.computeIfAbsent(peerKey, k -> {
                Map<String, Object> s = new HashMap<>();
                s.put("peerEmail", k);
                s.put("peerName", peerName);
                s.put("isAdminSupport", false);
                s.put("latestMessage", m.getMessage());
                s.put("latestTime", m.getCreatedAt().toString());
                s.put("unreadCount", 0L);
                s.put("productId", m.getProductId() != null ? m.getProductId() : "");
                s.put("productName", m.getProductName() != null ? m.getProductName() : "");
                return s;
            });
            if (!m.isRead() && m.getRecipientEmail() != null && m.getRecipientEmail().equalsIgnoreCase(myEmail)) {
                slot.put("unreadCount", ((Long) slot.get("unreadCount")) + 1L);
            }
        }

        return ResponseEntity.ok(ApiResponse.ok("Conversations loaded", new ArrayList<>(convos.values())));
    }

    // ── USER: Get private 1-on-1 DM messages with a specific peer (or Admin Support)
    @GetMapping("/dm")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPrivateDm(
            @RequestParam(required = false, defaultValue = "ADMIN_SUPPORT") String peerEmail,
            Authentication auth) {
        String myEmail = auth.getName();
        List<ChatMessage> msgs;
        if (peerEmail == null || peerEmail.isBlank() || "ADMIN_SUPPORT".equalsIgnoreCase(peerEmail)) {
            msgs = chatRepo.findSupportThreadByUserEmail(myEmail);
        } else {
            msgs = chatRepo.findPrivateDmThread(myEmail, peerEmail.trim());
        }
        return ResponseEntity.ok(ApiResponse.ok("Private thread loaded", msgs.stream().map(this::toMap).toList()));
    }

    // ── USER: Legacy support thread endpoint (kept for backward compatibility) ─
    @GetMapping("/thread")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyThread(Authentication auth) {
        String email = auth.getName();
        List<ChatMessage> msgs = chatRepo.findSupportThreadByUserEmail(email);
        return ResponseEntity.ok(ApiResponse.ok("Thread loaded", msgs.stream().map(this::toMap).toList()));
    }

    // ── USER: Mark private DM messages as read ────────────────────────────────
    @PostMapping("/mark-read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @RequestParam(required = false, defaultValue = "ADMIN_SUPPORT") String peerEmail,
            Authentication auth) {
        String myEmail = auth.getName();
        List<ChatMessage> unread;
        if (peerEmail == null || peerEmail.isBlank() || "ADMIN_SUPPORT".equalsIgnoreCase(peerEmail)) {
            unread = chatRepo.findSupportThreadByUserEmail(myEmail).stream()
                    .filter(m -> !m.isRead() && "ADMIN".equalsIgnoreCase(m.getSenderRole()))
                    .toList();
        } else {
            unread = chatRepo.findPrivateDmThread(myEmail, peerEmail.trim()).stream()
                    .filter(m -> !m.isRead() && !m.getSenderEmail().equalsIgnoreCase(myEmail))
                    .toList();
        }
        unread.forEach(m -> m.setRead(true));
        chatRepo.saveAll(unread);
        return ResponseEntity.ok(ApiResponse.ok("Marked as read", null));
    }

    // ── ADMIN: Get all support threads + B2B buyer-seller negotiation threads ─
    @GetMapping("/admin/inbox")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminInbox() {
        List<String> userEmails = chatRepo.findDistinctUserSenders();
        long unreadTotal = chatRepo.countUnreadForAdmin();
        List<Map<String, Object>> threads = userEmails.stream().map(email -> {
            List<ChatMessage> msgs = chatRepo.findSupportThreadByUserEmail(email);
            ChatMessage latest = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
            AppUser u = userRepo.findByEmailIgnoreCase(email).orElse(null);
            long unread = msgs.stream().filter(m -> !m.isRead() && "USER".equals(m.getSenderRole())).count();
            Map<String, Object> t = new HashMap<>();
            t.put("userEmail", email);
            t.put("userName", u != null ? u.getFullName() : email);
            t.put("unreadCount", unread);
            t.put("latestMessage", latest != null ? latest.getMessage() : "");
            t.put("latestTime", latest != null ? latest.getCreatedAt().toString() : "");
            t.put("messageCount", msgs.size());
            return t;
        }).toList();

        // Also build B2B Buyer <-> Seller direct chat pairs for Admin oversight
        List<ChatMessage> allMsgs = chatRepo.findAllByOrderByCreatedAtDesc();
        Map<String, Map<String, Object>> b2bPairs = new LinkedHashMap<>();
        for (ChatMessage m : allMsgs) {
            if (m.getRecipientEmail() == null || m.getRecipientEmail().isBlank()) continue;
            if ("ADMIN".equalsIgnoreCase(m.getSenderRole())) continue;
            String a = m.getSenderEmail().toLowerCase();
            String b = m.getRecipientEmail().toLowerCase();
            String pairKey = a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
            b2bPairs.computeIfAbsent(pairKey, k -> {
                String[] parts = k.split("\\|");
                Map<String, Object> p = new HashMap<>();
                p.put("emailA", parts[0]);
                p.put("emailB", parts[1]);
                p.put("latestMessage", m.getMessage());
                p.put("productName", m.getProductName() != null ? m.getProductName() : "");
                p.put("latestTime", m.getCreatedAt().toString());
                return p;
            });
        }

        return ResponseEntity.ok(ApiResponse.ok("Admin inbox", Map.of(
                "threads", threads,
                "b2bThreads", new ArrayList<>(b2bPairs.values()),
                "totalUnread", unreadTotal
        )));
    }

    // ── ADMIN: Inspect any B2B Buyer <-> Seller thread ────────────────────────
    @GetMapping("/admin/b2b-thread")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getB2bThread(
            @RequestParam String emailA,
            @RequestParam String emailB) {
        List<ChatMessage> msgs = chatRepo.findPrivateDmThread(emailA, emailB);
        return ResponseEntity.ok(ApiResponse.ok("B2B thread loaded", msgs.stream().map(this::toMap).toList()));
    }

    // ── ADMIN: Get specific user support thread ───────────────────────────────
    @GetMapping("/admin/thread/{userEmail}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserThread(
            @PathVariable String userEmail) {
        List<ChatMessage> msgs = chatRepo.findSupportThreadByUserEmail(userEmail);
        msgs.stream().filter(m -> !m.isRead() && "USER".equals(m.getSenderRole()))
                .forEach(m -> m.setRead(true));
        chatRepo.saveAll(msgs);
        return ResponseEntity.ok(ApiResponse.ok("Thread loaded", msgs.stream().map(this::toMap).toList()));
    }

    // ── ADMIN: Reply to user ──────────────────────────────────────────────────
    @PostMapping("/admin/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> adminReply(
            @RequestBody Map<String, String> req, Authentication auth) {
        String adminEmail = auth.getName();
        AppUser admin = userRepo.findByEmailIgnoreCase(adminEmail).orElse(null);
        String adminName = admin != null ? admin.getFullName() + " (Admin)" : "Platform Admin";

        String recipientEmail = req.getOrDefault("recipientEmail", "").trim();
        String messageText = req.getOrDefault("message", "").trim();
        if (recipientEmail.isBlank() || messageText.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("recipientEmail and message required."));
        }

        ChatMessage reply = new ChatMessage();
        reply.setSenderEmail(adminEmail);
        reply.setSenderName(adminName);
        reply.setSenderRole("ADMIN");
        reply.setRecipientEmail(recipientEmail);
        reply.setMessage(messageText);
        reply.setMessageType("TEXT");
        chatRepo.save(reply);

        realtimeEventService.publish("ADMIN_REPLY",
                "Admin replied to " + recipientEmail + ": " + (messageText.length() > 60 ? messageText.substring(0, 60) + "…" : messageText),
                adminName);

        return ResponseEntity.ok(ApiResponse.ok("Reply sent", toMap(reply)));
    }

    // ── Unread count for current user ────────────────────────────────────────
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount(Authentication auth) {
        String email = auth.getName();
        AppUser user = userRepo.findByEmailIgnoreCase(email).orElse(null);
        boolean isAdmin = user != null && "ADMIN".equals(user.getRole());
        long count = isAdmin
                ? chatRepo.countUnreadForAdmin()
                : chatRepo.countUnreadForUser(email);
        return ResponseEntity.ok(ApiResponse.ok("Unread count", Map.of("unreadCount", count)));
    }

    private Map<String, Object> toMap(ChatMessage m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId());
        map.put("senderEmail", m.getSenderEmail());
        map.put("senderName", m.getSenderName());
        map.put("senderRole", m.getSenderRole());
        map.put("recipientEmail", m.getRecipientEmail() != null ? m.getRecipientEmail() : "");
        map.put("message", m.getMessage());
        map.put("messageType", m.getMessageType());
        map.put("productId", m.getProductId() != null ? m.getProductId() : "");
        map.put("productName", m.getProductName() != null ? m.getProductName() : "");
        map.put("read", m.isRead());
        map.put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().toString() : "");
        return map;
    }
}
