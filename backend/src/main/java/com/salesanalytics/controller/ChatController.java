package com.salesanalytics.controller;

import com.salesanalytics.dto.ApiResponse;
import com.salesanalytics.entity.ChatMessage;
import com.salesanalytics.entity.AppUser;
import com.salesanalytics.repository.ChatMessageRepository;
import com.salesanalytics.repository.AppUserRepository;
import com.salesanalytics.service.RealtimeEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    // ── USER: Send a message to admin ────────────────────────────────────────
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

        ChatMessage msg = new ChatMessage();
        msg.setSenderEmail(senderEmail);
        msg.setSenderName(sender.getFullName());
        msg.setSenderRole(sender.getRole());
        msg.setMessage(messageText.trim());
        msg.setMessageType(messageType);
        if (productIdObj != null) {
            msg.setProductId(((Number) productIdObj).longValue());
            msg.setProductName(productName);
        }
        // USER messages go to admin (no specific recipient = admin inbox)
        if ("ADMIN".equals(sender.getRole())) {
            // Admin replying: set recipient
            String recipientEmail = (String) req.getOrDefault("recipientEmail", "");
            if (!recipientEmail.isBlank()) msg.setRecipientEmail(recipientEmail);
        }
        chatRepo.save(msg);

        // Push real-time notification via SSE
        realtimeEventService.publish("CHAT_MESSAGE",
                "New message from " + sender.getFullName() + ": " + (messageText.length() > 60 ? messageText.substring(0, 60) + "…" : messageText),
                sender.getFullName());

        return ResponseEntity.ok(ApiResponse.ok("Message sent", Map.of(
                "id", msg.getId(),
                "senderName", sender.getFullName(),
                "senderRole", sender.getRole(),
                "message", msg.getMessage(),
                "createdAt", msg.getCreatedAt().toString()
        )));
    }

    // ── USER: Get own chat thread ─────────────────────────────────────────────
    @GetMapping("/thread")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyThread(Authentication auth) {
        String email = auth.getName();
        List<ChatMessage> msgs = chatRepo.findThreadByUserEmail(email);
        return ResponseEntity.ok(ApiResponse.ok("Thread loaded", msgs.stream().map(this::toMap).toList()));
    }

    // ── USER: Mark messages from admin as read ────────────────────────────────
    @PostMapping("/mark-read")
    public ResponseEntity<ApiResponse<Void>> markRead(Authentication auth) {
        String email = auth.getName();
        List<ChatMessage> unread = chatRepo.findThreadByUserEmail(email).stream()
                .filter(m -> !m.isRead() && "ADMIN".equals(m.getSenderRole()))
                .toList();
        unread.forEach(m -> m.setRead(true));
        chatRepo.saveAll(unread);
        return ResponseEntity.ok(ApiResponse.ok("Marked as read", null));
    }

    // ── ADMIN: Get all user threads (inbox) ───────────────────────────────────
    @GetMapping("/admin/inbox")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminInbox() {
        List<String> userEmails = chatRepo.findDistinctUserSenders();
        long unreadTotal = chatRepo.countUnreadForAdmin();
        List<Map<String, Object>> threads = userEmails.stream().map(email -> {
            List<ChatMessage> msgs = chatRepo.findThreadByUserEmail(email);
            ChatMessage latest = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
            AppUser u = userRepo.findByEmailIgnoreCase(email).orElse(null);
            long unread = msgs.stream().filter(m -> !m.isRead() && "USER".equals(m.getSenderRole())).count();
            return Map.<String, Object>of(
                    "userEmail", email,
                    "userName", u != null ? u.getFullName() : email,
                    "unreadCount", unread,
                    "latestMessage", latest != null ? latest.getMessage() : "",
                    "latestTime", latest != null ? latest.getCreatedAt().toString() : "",
                    "messageCount", msgs.size()
            );
        }).toList();
        return ResponseEntity.ok(ApiResponse.ok("Admin inbox", Map.of(
                "threads", threads,
                "totalUnread", unreadTotal
        )));
    }

    // ── ADMIN: Get specific user thread ──────────────────────────────────────
    @GetMapping("/admin/thread/{userEmail}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserThread(
            @PathVariable String userEmail) {
        List<ChatMessage> msgs = chatRepo.findThreadByUserEmail(userEmail);
        // Mark user messages as read
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
        AppUser admin = userRepo.findByEmailIgnoreCase(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        String recipientEmail = req.getOrDefault("recipientEmail", "").trim();
        String messageText = req.getOrDefault("message", "").trim();
        if (recipientEmail.isBlank() || messageText.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("recipientEmail and message required."));
        }

        ChatMessage reply = new ChatMessage();
        reply.setSenderEmail(adminEmail);
        reply.setSenderName(admin.getFullName() + " (Admin)");
        reply.setSenderRole("ADMIN");
        reply.setRecipientEmail(recipientEmail);
        reply.setMessage(messageText);
        reply.setMessageType("TEXT");
        chatRepo.save(reply);

        realtimeEventService.publish("ADMIN_REPLY",
                "Admin replied to " + recipientEmail + ": " + (messageText.length() > 60 ? messageText.substring(0, 60) + "…" : messageText),
                admin.getFullName());

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
        return Map.of(
                "id", m.getId(),
                "senderEmail", m.getSenderEmail(),
                "senderName", m.getSenderName(),
                "senderRole", m.getSenderRole(),
                "message", m.getMessage(),
                "messageType", m.getMessageType(),
                "productId", m.getProductId() != null ? m.getProductId() : "",
                "productName", m.getProductName() != null ? m.getProductName() : "",
                "read", m.isRead(),
                "createdAt", m.getCreatedAt().toString()
        );
    }
}
