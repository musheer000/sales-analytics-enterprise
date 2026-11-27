package com.salesanalytics.controller;

import com.salesanalytics.dto.ApiResponse;
import com.salesanalytics.entity.*;
import com.salesanalytics.repository.*;
import com.salesanalytics.service.MarketplaceAlgorithmService;
import com.salesanalytics.service.RealtimeEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Wholesale & B2B RFQ Negotiation Microservice Module (/api/wholesale/**).
 * Handles bulk quotes, price negotiation, counter-offers, quality specs,
 * cross-border delivery, and converting accepted quotes into orders.
 */
@RestController
@RequestMapping("/api/wholesale")
@CrossOrigin(origins = "*")
@Transactional
public class WholesaleController {

    private final WholesaleQuoteRepository quoteRepo;
    private final ProductRepository productRepo;
    private final AppUserRepository userRepo;
    private final UserOrderRepository orderRepo;
    private final MarketplaceAlgorithmService algorithmService;
    private final RealtimeEventService realtimeEventService;

    private static final AtomicLong rfqCounter = new AtomicLong(System.currentTimeMillis() % 100000);

    public WholesaleController(WholesaleQuoteRepository quoteRepo,
                               ProductRepository productRepo,
                               AppUserRepository userRepo,
                               UserOrderRepository orderRepo,
                               MarketplaceAlgorithmService algorithmService,
                               RealtimeEventService realtimeEventService) {
        this.quoteRepo = quoteRepo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
        this.algorithmService = algorithmService;
        this.realtimeEventService = realtimeEventService;
    }

    // ── BUYER: Submit a Bulk RFQ / Price Negotiation ─────────────────────────
    @PostMapping("/quotes")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createQuote(
            @RequestBody Map<String, Object> req,
            Authentication auth) {

        String buyerEmail = auth.getName();
        AppUser buyer = userRepo.findByEmailIgnoreCase(buyerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Buyer not found"));

        Long productId = ((Number) req.get("productId")).longValue();
        int requestedQty = ((Number) req.getOrDefault("requestedQty", 10)).intValue();
        BigDecimal proposedPrice = new BigDecimal(String.valueOf(req.get("proposedUnitPrice")));
        String buyerCountry = String.valueOf(req.getOrDefault("buyerCountry", "India")).trim();
        String qualityNotes = String.valueOf(req.getOrDefault("qualityNotes", "")).trim();
        String deliveryAddress = String.valueOf(req.getOrDefault("deliveryAddress", "")).trim();

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Enforce global trade compliance (India <-> Pakistan restriction)
        algorithmService.validateTradeCompliance(product.getOriginCountry(), buyerCountry);

        if (requestedQty < 1) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Quantity must be at least 1."));
        }
        if (proposedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Proposed unit price must be positive."));
        }

        String quoteRef = "RFQ-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%05d", rfqCounter.incrementAndGet() % 100000);

        WholesaleQuote q = new WholesaleQuote();
        q.setQuoteRef(quoteRef);
        q.setProduct(product);
        q.setBuyerEmail(buyerEmail);
        q.setBuyerName(buyer.getFullName());
        q.setBuyerCountry(buyerCountry.isBlank() ? "India" : buyerCountry);
        q.setSellerEmail(product.getSellerEmail());
        q.setSellerName(product.getSellerName());
        q.setRequestedQty(requestedQty);
        q.setOriginalUnitPrice(product.getWholesalePrice() != null ? product.getWholesalePrice() : product.getPrice());
        q.setProposedUnitPrice(proposedPrice);
        q.setQualityNotes(qualityNotes);
        q.setDeliveryAddress(deliveryAddress);
        q.setStatus("PENDING");
        quoteRepo.save(q);

        realtimeEventService.publish("RFQ_CREATED",
                "New bulk quote " + quoteRef + " (" + requestedQty + " " + product.getUnitType() + " of " + product.getName() + ") from " + buyerCountry,
                buyer.getFullName());

        return ResponseEntity.ok(ApiResponse.ok("Bulk quote request sent to " + product.getSellerName(), toMap(q)));
    }

    // ── USER (BUYER & SELLER): View my sent & received quotes ────────────────
    @GetMapping("/quotes/my")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyQuotes(Authentication auth) {
        String email = auth.getName();
        List<WholesaleQuote> sent = quoteRepo.findByBuyerEmailWithProduct(email);
        List<WholesaleQuote> received = quoteRepo.findBySellerEmailWithProduct(email);
        return ResponseEntity.ok(ApiResponse.ok("Quotes loaded", Map.of(
                "sentQuotes", sent.stream().map(this::toMap).toList(),
                "receivedQuotes", received.stream().map(this::toMap).toList()
        )));
    }

    // ── ADMIN: View all wholesale RFQs across the platform ───────────────────
    @GetMapping("/quotes/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllQuotesAdmin() {
        List<WholesaleQuote> all = quoteRepo.findAllWithProduct();
        return ResponseEntity.ok(ApiResponse.ok("All wholesale quotes loaded", all.stream().map(this::toMap).toList()));
    }

    // ── SELLER or ADMIN: Respond to quote (ACCEPT, COUNTER, REJECT) ──────────
    @PutMapping("/quotes/{id}/respond")
    public ResponseEntity<ApiResponse<Map<String, Object>>> respondToQuote(
            @PathVariable Long id,
            @RequestBody Map<String, Object> req,
            Authentication auth) {

        String actorEmail = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        WholesaleQuote q = quoteRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found"));

        boolean isSeller = q.getSellerEmail() != null && q.getSellerEmail().equalsIgnoreCase(actorEmail);
        if (!isAdmin && !isSeller) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only the product seller or Admin can respond to this quote."));
        }

        String action = String.valueOf(req.getOrDefault("action", "")).toUpperCase();
        String responseNote = String.valueOf(req.getOrDefault("sellerResponse", "")).trim();
        q.setSellerResponse(responseNote);

        switch (action) {
            case "ACCEPT" -> {
                q.setStatus("ACCEPTED");
                q.setAgreedUnitPrice(q.getProposedUnitPrice());
            }
            case "COUNTER" -> {
                if (req.get("counterUnitPrice") == null) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("counterUnitPrice is required for a counter-offer."));
                }
                BigDecimal counter = new BigDecimal(String.valueOf(req.get("counterUnitPrice")));
                q.setCounterUnitPrice(counter);
                q.setStatus("COUNTERED");
            }
            case "REJECT" -> q.setStatus("REJECTED");
            default -> {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid action. Use ACCEPT, COUNTER, or REJECT."));
            }
        }
        quoteRepo.save(q);

        realtimeEventService.publish("RFQ_UPDATED",
                "Quote " + q.getQuoteRef() + " is now " + q.getStatus(),
                actorEmail);

        return ResponseEntity.ok(ApiResponse.ok("Quote updated: " + q.getStatus(), toMap(q)));
    }

    // ── BUYER: Accept Seller's Counter-Offer ─────────────────────────────────
    @PutMapping("/quotes/{id}/accept-counter")
    public ResponseEntity<ApiResponse<Map<String, Object>>> acceptCounterOffer(
            @PathVariable Long id,
            Authentication auth) {

        WholesaleQuote q = quoteRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found"));
        if (!q.getBuyerEmail().equalsIgnoreCase(auth.getName())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only the buyer can accept this counter-offer."));
        }
        if (!"COUNTERED".equals(q.getStatus()) || q.getCounterUnitPrice() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No active counter-offer to accept."));
        }
        q.setStatus("ACCEPTED");
        q.setAgreedUnitPrice(q.getCounterUnitPrice());
        quoteRepo.save(q);
        return ResponseEntity.ok(ApiResponse.ok("Counter-offer accepted! Ready to place bulk order.", toMap(q)));
    }

    // ── BUYER: Convert Accepted Quote to Bulk Order ──────────────────────────
    @PostMapping("/quotes/{id}/convert-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> convertQuoteToOrder(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> req,
            Authentication auth) {

        WholesaleQuote q = quoteRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found"));
        if (!q.getBuyerEmail().equalsIgnoreCase(auth.getName())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Unauthorized."));
        }
        if (!"ACCEPTED".equals(q.getStatus())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Only ACCEPTED quotes can be converted to an order."));
        }

        Product product = q.getProduct();
        BigDecimal unitPrice = q.getAgreedUnitPrice() != null ? q.getAgreedUnitPrice() : q.getProposedUnitPrice();
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(q.getRequestedQty()));

        String paymentMethod = req != null ? String.valueOf(req.getOrDefault("paymentMethod", "COD")) : "COD";
        String address = req != null && req.get("shippingAddress") != null
                ? String.valueOf(req.get("shippingAddress"))
                : (q.getDeliveryAddress() != null ? q.getDeliveryAddress() : "");

        String orderRef = "UO-BULK-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", rfqCounter.incrementAndGet() % 10000);

        UserOrder order = new UserOrder();
        order.setOrderRef(orderRef);
        order.setUserEmail(q.getBuyerEmail());
        order.setUserName(q.getBuyerName());
        order.setProduct(product);
        order.setQuantity(q.getRequestedQty());
        order.setUnitPrice(unitPrice);
        order.setTotalAmount(total);
        order.setPaymentMethod(paymentMethod);
        order.setShippingAddress(address);
        order.setBuyerCountry(q.getBuyerCountry());
        order.setBuyerRegion(UserPortalController.mapCountryToRegion(q.getBuyerCountry()));
        order.setOrderType("WHOLESALE");
        order.setQuoteRef(q.getQuoteRef());
        order.setNotes("Bulk RFQ Order (" + q.getQuoteRef() + ") — " + (q.getQualityNotes() != null ? q.getQualityNotes() : ""));
        order.setStatus("PENDING");
        orderRepo.save(order);

        q.setStatus("ORDERED");
        quoteRepo.save(q);

        realtimeEventService.publish("WHOLESALE_ORDER",
                "Bulk order placed: " + orderRef + " (" + q.getRequestedQty() + " units, " + q.getBuyerCountry() + ")",
                q.getBuyerName());

        return ResponseEntity.ok(ApiResponse.ok("Bulk wholesale order placed!", Map.of(
                "orderRef", orderRef,
                "totalAmount", total,
                "status", order.getStatus()
        )));
    }

    private Map<String, Object> toMap(WholesaleQuote q) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", q.getId());
        m.put("quoteRef", q.getQuoteRef());
        m.put("productId", q.getProduct().getId());
        m.put("productName", q.getProduct().getName());
        m.put("unitType", q.getProduct().getUnitType());
        m.put("buyerEmail", q.getBuyerEmail());
        m.put("buyerName", q.getBuyerName());
        m.put("buyerCountry", q.getBuyerCountry());
        m.put("sellerEmail", q.getSellerEmail() != null ? q.getSellerEmail() : "ADMIN_SUPPORT");
        m.put("sellerName", q.getSellerName());
        m.put("requestedQty", q.getRequestedQty());
        m.put("originalUnitPrice", q.getOriginalUnitPrice());
        m.put("proposedUnitPrice", q.getProposedUnitPrice());
        m.put("counterUnitPrice", q.getCounterUnitPrice());
        m.put("agreedUnitPrice", q.getAgreedUnitPrice());
        m.put("status", q.getStatus());
        m.put("qualityNotes", q.getQualityNotes() != null ? q.getQualityNotes() : "");
        m.put("sellerResponse", q.getSellerResponse() != null ? q.getSellerResponse() : "");
        m.put("deliveryAddress", q.getDeliveryAddress() != null ? q.getDeliveryAddress() : "");
        m.put("createdAt", q.getCreatedAt() != null ? q.getCreatedAt().toString() : "");
        return m;
    }
}
