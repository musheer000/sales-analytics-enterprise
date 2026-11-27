package com.salesanalytics.controller;

import com.salesanalytics.dto.ApiResponse;
import com.salesanalytics.entity.*;
import com.salesanalytics.repository.*;
import com.salesanalytics.service.RealtimeEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
@Transactional
public class UserPortalController {

    private final ProductRepository productRepo;
    private final UserOrderRepository userOrderRepo;
    private final ProductReviewRepository reviewRepo;
    private final AppUserRepository userRepo;
    private final RealtimeEventService realtimeEventService;

    private static final AtomicLong orderCounter = new AtomicLong(System.currentTimeMillis() % 100000);

    public UserPortalController(ProductRepository productRepo,
                                UserOrderRepository userOrderRepo,
                                ProductReviewRepository reviewRepo,
                                AppUserRepository userRepo,
                                RealtimeEventService realtimeEventService) {
        this.productRepo = productRepo;
        this.userOrderRepo = userOrderRepo;
        this.reviewRepo = reviewRepo;
        this.userRepo = userRepo;
        this.realtimeEventService = realtimeEventService;
    }

    // ── PRODUCTS: Browse all (open to any authenticated user) ────────────────
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> browseProducts() {
        List<Product> products = productRepo.findAllWithCategory();
        List<Map<String, Object>> result = products.stream().map(p -> {
            Double avgRating = reviewRepo.findAverageRatingByProductId(p.getId());
            long reviewCount = reviewRepo.countByProductId(p.getId());
            return Map.<String, Object>of(
                    "id", p.getId(),
                    "name", p.getName(),
                    "sku", p.getSku(),
                    "price", p.getPrice(),
                    "stockQuantity", p.getStockQuantity(),
                    "description", p.getDescription() != null ? p.getDescription() : "",
                    "categoryName", p.getCategory() != null ? p.getCategory().getName() : "",
                    "avgRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0,
                    "reviewCount", reviewCount,
                    "inStock", p.getStockQuantity() > 0
            );
        }).toList();
        return ResponseEntity.ok(ApiResponse.ok("Products loaded", result));
    }

    // ── REVIEWS: Get reviews for a product ───────────────────────────────────
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductReviews(
            @PathVariable Long productId) {
        List<ProductReview> reviews = reviewRepo.findByProductIdOrderByCreatedAtDesc(productId);
        Double avg = reviewRepo.findAverageRatingByProductId(productId);
        long count = reviewRepo.countByProductId(productId);
        List<Map<String, Object>> reviewMaps = reviews.stream().map(r -> Map.<String, Object>of(
                "id", r.getId(),
                "userName", r.getUserName(),
                "rating", r.getRating(),
                "reviewText", r.getReviewText(),
                "verifiedPurchase", r.isVerifiedPurchase(),
                "helpfulCount", r.getHelpfulCount(),
                "createdAt", r.getCreatedAt().toString()
        )).toList();
        return ResponseEntity.ok(ApiResponse.ok("Reviews loaded", Map.of(
                "reviews", reviewMaps,
                "averageRating", avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0,
                "totalReviews", count
        )));
    }

    // ── REVIEWS: Submit a review ──────────────────────────────────────────────
    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitReview(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> req,
            Authentication auth) {

        String userEmail = auth.getName();
        AppUser user = userRepo.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Check if already reviewed
        if (reviewRepo.findByProductIdAndUserEmail(productId, userEmail).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("You have already reviewed this product."));
        }

        int rating = ((Number) req.getOrDefault("rating", 0)).intValue();
        if (rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Rating must be between 1 and 5."));
        }
        String reviewText = (String) req.getOrDefault("reviewText", "");
        if (reviewText.trim().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Review text is required."));
        }

        // Check if user has purchased this product
        boolean hasPurchased = userOrderRepo.findByUserEmailOrderByCreatedAtDesc(userEmail).stream()
                .anyMatch(o -> "PAID".equals(o.getStatus()) &&
                        o.getProduct().getId().equals(productId));

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUserEmail(userEmail);
        review.setUserName(user.getFullName());
        review.setRating(rating);
        review.setReviewText(reviewText.trim());
        review.setVerifiedPurchase(hasPurchased);
        reviewRepo.save(review);

        return ResponseEntity.ok(ApiResponse.ok("Review submitted", Map.of(
                "id", review.getId(),
                "rating", review.getRating(),
                "verifiedPurchase", review.isVerifiedPurchase()
        )));
    }

    // ── REVIEWS: Mark helpful ─────────────────────────────────────────────────
    @PostMapping("/reviews/{reviewId}/helpful")
    public ResponseEntity<ApiResponse<Void>> markHelpful(@PathVariable Long reviewId) {
        ProductReview r = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        r.setHelpfulCount(r.getHelpfulCount() + 1);
        reviewRepo.save(r);
        return ResponseEntity.ok(ApiResponse.ok("Marked helpful", null));
    }

    // ── ORDERS: Place a new order ─────────────────────────────────────────────
    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<Map<String, Object>>> placeOrder(
            @RequestBody Map<String, Object> req,
            Authentication auth) {

        String userEmail = auth.getName();
        AppUser user = userRepo.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Long productId = ((Number) req.get("productId")).longValue();
        int quantity = ((Number) req.getOrDefault("quantity", 1)).intValue();
        String paymentMethod = (String) req.getOrDefault("paymentMethod", "COD");
        String shippingAddress = (String) req.getOrDefault("shippingAddress", "");
        String notes = (String) req.getOrDefault("notes", "");

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (product.getStockQuantity() < quantity) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Only " + product.getStockQuantity() + " units available."));
        }

        BigDecimal unitPrice = product.getPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        // Generate order ref
        String orderRef = "UO-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%05d", orderCounter.incrementAndGet() % 100000);

        UserOrder order = new UserOrder();
        order.setOrderRef(orderRef);
        order.setUserEmail(userEmail);
        order.setUserName(user.getFullName());
        order.setProduct(product);
        order.setQuantity(quantity);
        order.setUnitPrice(unitPrice);
        order.setTotalAmount(totalAmount);
        order.setPaymentMethod(paymentMethod);
        order.setShippingAddress(shippingAddress);
        order.setNotes(notes);
        order.setStatus("COD".equals(paymentMethod) ? "PENDING" : "PENDING");
        userOrderRepo.save(order);

        realtimeEventService.publish("USER_ORDER",
                "New user order: " + orderRef + " — " + product.getName() + " by " + user.getFullName(),
                user.getFullName());

        return ResponseEntity.ok(ApiResponse.ok("Order placed successfully", Map.of(
                "orderId", order.getId(),
                "orderRef", orderRef,
                "totalAmount", totalAmount,
                "status", order.getStatus()
        )));
    }

    // ── ORDERS: Confirm payment (Razorpay callback) ───────────────────────────
    @PostMapping("/orders/{orderRef}/confirm-payment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmPayment(
            @PathVariable String orderRef,
            @RequestBody Map<String, String> req) {

        UserOrder order = userOrderRepo.findByOrderRef(orderRef)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderRef));

        order.setRazorpayOrderId(req.getOrDefault("razorpay_order_id", ""));
        order.setRazorpayPaymentId(req.getOrDefault("razorpay_payment_id", ""));
        order.setStatus("PAID");
        userOrderRepo.save(order);

        return ResponseEntity.ok(ApiResponse.ok("Payment confirmed", Map.of(
                "orderRef", orderRef,
                "status", "PAID"
        )));
    }

    // ── ORDERS: Get user's own order history ──────────────────────────────────
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyOrders(Authentication auth) {
        String userEmail = auth.getName();
        List<UserOrder> orders = userOrderRepo.findByUserEmailWithProduct(userEmail);
        return ResponseEntity.ok(ApiResponse.ok("Order history", orders.stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("orderRef", o.getOrderRef());
            m.put("productName", o.getProduct().getName());
            m.put("productId", o.getProduct().getId());
            m.put("quantity", o.getQuantity());
            m.put("unitPrice", o.getUnitPrice());
            m.put("totalAmount", o.getTotalAmount());
            m.put("paymentMethod", o.getPaymentMethod());
            m.put("status", o.getStatus());
            m.put("shippingAddress", o.getShippingAddress() != null ? o.getShippingAddress() : "");
            m.put("notes", o.getNotes() != null ? o.getNotes() : "");
            m.put("createdAt", o.getCreatedAt().toString());
            return m;
        }).toList()));
    }

    // ── ADMIN: Get all user orders ────────────────────────────────────────────
    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllUserOrders() {
        List<UserOrder> orders = userOrderRepo.findAllByOrderByCreatedAtDesc();
        long pending = userOrderRepo.countByStatus("PENDING");
        long paid = userOrderRepo.countByStatus("PAID");
        return ResponseEntity.ok(ApiResponse.ok("All user orders", Map.of(
                "orders", orders.stream().map(o -> Map.<String, Object>of(
                        "id", o.getId(),
                        "orderRef", o.getOrderRef(),
                        "userName", o.getUserName(),
                        "userEmail", o.getUserEmail(),
                        "productName", o.getProduct().getName(),
                        "quantity", o.getQuantity(),
                        "totalAmount", o.getTotalAmount(),
                        "paymentMethod", o.getPaymentMethod(),
                        "status", o.getStatus(),
                        "createdAt", o.getCreatedAt().toString()
                )).toList(),
                "totalOrders", orders.size(),
                "pendingOrders", pending,
                "paidOrders", paid
        )));
    }

    // ── ADMIN: Update order status ────────────────────────────────────────────
    @PutMapping("/admin/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> req) {
        UserOrder order = userOrderRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        order.setStatus(req.getOrDefault("status", order.getStatus()).toUpperCase());
        userOrderRepo.save(order);
        return ResponseEntity.ok(ApiResponse.ok("Order status updated", null));
    }

    // ── USER: Dashboard summary (own stats) ───────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserDashboard(Authentication auth) {
        String userEmail = auth.getName();
        List<UserOrder> orders = userOrderRepo.findByUserEmailOrderByCreatedAtDesc(userEmail);
        long totalOrders = orders.size();
        long paidOrders = orders.stream().filter(o -> "PAID".equals(o.getStatus())).count();
        BigDecimal totalSpent = orders.stream()
                .filter(o -> "PAID".equals(o.getStatus()))
                .map(UserOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<UserOrder> recentOrders = orders.stream().limit(5).toList();
        List<ProductReview> myReviews = reviewRepo.findByUserEmailOrderByCreatedAtDesc(userEmail);

        return ResponseEntity.ok(ApiResponse.ok("User dashboard", Map.of(
                "totalOrders", totalOrders,
                "paidOrders", paidOrders,
                "pendingOrders", totalOrders - paidOrders,
                "totalSpent", totalSpent,
                "reviewsGiven", myReviews.size(),
                "recentOrders", recentOrders.stream().limit(5).map(o -> Map.<String, Object>of(
                        "orderRef", o.getOrderRef(),
                        "productName", o.getProduct().getName(),
                        "totalAmount", o.getTotalAmount(),
                        "status", o.getStatus(),
                        "createdAt", o.getCreatedAt().toString()
                )).toList()
        )));
    }
}
