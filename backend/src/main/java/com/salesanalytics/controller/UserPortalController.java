package com.salesanalytics.controller;

import com.salesanalytics.dto.ApiResponse;
import com.salesanalytics.entity.*;
import com.salesanalytics.repository.*;
import com.salesanalytics.service.MarketplaceAlgorithmService;
import com.salesanalytics.service.RealtimeEventService;
import org.springframework.cache.annotation.CacheEvict;
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
    private final CategoryRepository categoryRepo;
    private final UserOrderRepository userOrderRepo;
    private final ProductReviewRepository reviewRepo;
    private final AppUserRepository userRepo;
    private final WholesaleQuoteRepository quoteRepo;
    private final SaleRepository saleRepo;
    private final CustomerRepository customerRepo;
    private final ChatMessageRepository chatRepo;
    private final MarketplaceAlgorithmService algorithmService;
    private final RealtimeEventService realtimeEventService;

    private static final AtomicLong orderCounter = new AtomicLong(System.currentTimeMillis() % 100000);

    public UserPortalController(ProductRepository productRepo,
                                CategoryRepository categoryRepo,
                                UserOrderRepository userOrderRepo,
                                ProductReviewRepository reviewRepo,
                                AppUserRepository userRepo,
                                WholesaleQuoteRepository quoteRepo,
                                SaleRepository saleRepo,
                                CustomerRepository customerRepo,
                                ChatMessageRepository chatRepo,
                                MarketplaceAlgorithmService algorithmService,
                                RealtimeEventService realtimeEventService) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.userOrderRepo = userOrderRepo;
        this.reviewRepo = reviewRepo;
        this.userRepo = userRepo;
        this.quoteRepo = quoteRepo;
        this.saleRepo = saleRepo;
        this.customerRepo = customerRepo;
        this.chatRepo = chatRepo;
        this.algorithmService = algorithmService;
        this.realtimeEventService = realtimeEventService;
    }

    // ── CATEGORIES: List for seller product submission ───────────────────────
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listCategories() {
        List<Map<String, Object>> cats = categoryRepo.findAll().stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "name", c.getName()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Categories loaded", cats));
    }

    // ── PRODUCTS: Browse approved retail & wholesale marketplace ─────────────
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> browseProducts() {
        List<Product> products = productRepo.findApprovedWithCategory();
        List<Map<String, Object>> result = products.stream().map(this::toProductMap).toList();
        return ResponseEntity.ok(ApiResponse.ok("Products loaded", result));
    }

    // ── SELLER: Submit a wholesale / bulk product listing (pending admin approval)
    @PostMapping("/products/submit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitProductListing(
            @RequestBody Map<String, Object> req,
            Authentication auth) {

        String sellerEmail = auth.getName();
        AppUser seller = userRepo.findByEmailIgnoreCase(sellerEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String name = String.valueOf(req.getOrDefault("name", "")).trim();
        if (name.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Product name is required."));
        }
        Long categoryId = req.get("categoryId") != null ? ((Number) req.get("categoryId")).longValue() : null;
        Category category = categoryId != null
                ? categoryRepo.findById(categoryId).orElseThrow(() -> new IllegalArgumentException("Category not found"))
                : categoryRepo.findAll().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("No categories available"));

        BigDecimal price = new BigDecimal(String.valueOf(req.getOrDefault("price", "0")));
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Price must be greater than 0."));
        }
        BigDecimal wholesalePrice = req.get("wholesalePrice") != null && !String.valueOf(req.get("wholesalePrice")).isBlank()
                ? new BigDecimal(String.valueOf(req.get("wholesalePrice")))
                : price.multiply(new BigDecimal("0.85"));

        int stock = ((Number) req.getOrDefault("stockQuantity", 100)).intValue();
        int moq = Math.max(1, ((Number) req.getOrDefault("moq", 1)).intValue());
        int bulkMinQty = Math.max(moq, ((Number) req.getOrDefault("bulkMinQty", 10)).intValue());
        String unitType = String.valueOf(req.getOrDefault("unitType", "pcs")).trim();
        String originCountry = String.valueOf(req.getOrDefault("originCountry", "India")).trim();
        String description = String.valueOf(req.getOrDefault("description", "")).trim();
        boolean negotiable = Boolean.parseBoolean(String.valueOf(req.getOrDefault("negotiable", true)));
        String imageUrl = req.get("imageUrl") != null ? String.valueOf(req.get("imageUrl")).trim() : null;
        String gradeSpec = String.valueOf(req.getOrDefault("gradeSpec", "")).trim();
        String packagingInfo = String.valueOf(req.getOrDefault("packagingInfo", "")).trim();
        String shelfLife = String.valueOf(req.getOrDefault("shelfLife", "")).trim();

        String sku = "B2B-" + Long.toHexString(System.currentTimeMillis()).toUpperCase();

        Product p = new Product();
        p.setName(name);
        p.setSku(sku);
        p.setPrice(price);
        p.setWholesalePrice(wholesalePrice);
        p.setStockQuantity(Math.max(1, stock));
        p.setMoq(moq);
        p.setBulkMinQty(bulkMinQty);
        p.setUnitType(unitType.isBlank() ? "pcs" : unitType);
        p.setOriginCountry(originCountry.isBlank() ? "India" : originCountry);
        p.setCategory(category);
        p.setDescription(description);
        p.setSellerEmail(sellerEmail);
        p.setSellerName(seller.getFullName());
        p.setNegotiable(negotiable);
        p.setImageUrl(imageUrl);
        p.setGradeSpec(gradeSpec);
        p.setPackagingInfo(packagingInfo);
        p.setShelfLife(shelfLife);
        // Admin/Owner listings auto-approve; user/supplier listings require Admin Accept/Reject
        p.setApprovalStatus("ADMIN".equalsIgnoreCase(seller.getRole()) ? "APPROVED" : "PENDING");
        productRepo.save(p);

        realtimeEventService.publish("PRODUCT_SUBMITTED",
                "New wholesale listing pending approval: " + p.getName() + " by " + seller.getFullName(),
                seller.getFullName());

        return ResponseEntity.ok(ApiResponse.ok(
                "APPROVED".equals(p.getApprovalStatus())
                        ? "Product published to marketplace"
                        : "Product submitted! Waiting for Admin approval.",
                toProductMap(p)));
    }

    // ── SELLER: View my submitted listings ──────────────────────────────────
    @GetMapping("/products/my-listings")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyListings(Authentication auth) {
        List<Product> list = productRepo.findBySellerEmailWithCategory(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("My listings loaded", list.stream().map(this::toProductMap).toList()));
    }

    // ── ADMIN: View product approval queue ──────────────────────────────────
    @GetMapping("/admin/products/approvals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductApprovals() {
        List<Product> pending = productRepo.findByApprovalStatusWithCategory("PENDING");
        List<Product> rejected = productRepo.findByApprovalStatusWithCategory("REJECTED");
        List<Product> approved = productRepo.findByApprovalStatusWithCategory("APPROVED");
        return ResponseEntity.ok(ApiResponse.ok("Approval queue loaded", Map.of(
                "pending", pending.stream().map(this::toProductMap).toList(),
                "rejected", rejected.stream().map(this::toProductMap).toList(),
                "approvedCount", approved.size(),
                "pendingCount", pending.size()
        )));
    }

    // ── ADMIN: Accept (Approve) a product listing ───────────────────────────
    @PutMapping("/admin/products/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveProduct(@PathVariable Long id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        p.setApprovalStatus("APPROVED");
        p.setRejectionReason(null);
        productRepo.save(p);
        realtimeEventService.publish("PRODUCT_APPROVED", "Product approved: " + p.getName(), "Admin");
        return ResponseEntity.ok(ApiResponse.ok("Product approved and live in marketplace", toProductMap(p)));
    }

    // ── ADMIN: Reject a product listing ─────────────────────────────────────
    @PutMapping("/admin/products/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectProduct(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> req) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        p.setApprovalStatus("REJECTED");
        p.setRejectionReason(req != null ? req.getOrDefault("reason", "Does not meet marketplace standards") : "Rejected by Admin");
        productRepo.save(p);
        realtimeEventService.publish("PRODUCT_REJECTED", "Product rejected: " + p.getName(), "Admin");
        return ResponseEntity.ok(ApiResponse.ok("Product rejected", toProductMap(p)));
    }

    private Map<String, Object> toProductMap(Product p) {
        Double avgRating = reviewRepo.findAverageRatingByProductId(p.getId());
        long reviewCount = reviewRepo.countByProductId(p.getId());
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("sku", p.getSku());
        m.put("price", p.getPrice());
        m.put("wholesalePrice", p.getWholesalePrice() != null ? p.getWholesalePrice() : p.getPrice().multiply(new BigDecimal("0.85")));
        m.put("moq", p.getMoq());
        m.put("bulkMinQty", p.getBulkMinQty());
        m.put("unitType", p.getUnitType());
        m.put("originCountry", p.getOriginCountry());
        m.put("sellerEmail", p.getSellerEmail() != null ? p.getSellerEmail() : "ADMIN_SUPPORT");
        m.put("sellerName", p.getSellerName());
        m.put("approvalStatus", p.getApprovalStatus());
        m.put("rejectionReason", p.getRejectionReason() != null ? p.getRejectionReason() : "");
        m.put("negotiable", p.isNegotiable());
        m.put("stockQuantity", p.getStockQuantity());
        m.put("description", p.getDescription() != null ? p.getDescription() : "");
        m.put("categoryName", p.getCategory() != null ? p.getCategory().getName() : "");
        m.put("avgRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
        m.put("reviewCount", reviewCount);
        m.put("inStock", p.getStockQuantity() > 0);
        m.put("imageUrl", p.getImageUrl() != null ? p.getImageUrl() : "");
        m.put("gradeSpec", p.getGradeSpec() != null ? p.getGradeSpec() : "");
        m.put("packagingInfo", p.getPackagingInfo() != null ? p.getPackagingInfo() : "");
        m.put("shelfLife", p.getShelfLife() != null ? p.getShelfLife() : "");
        m.put("trendingScore", algorithmService.computeTrendingScore(reviewCount * 10L, avgRating != null ? avgRating : 0.0, p.getStockQuantity()));
        return m;
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
    @CacheEvict(value = {"analyticsSummary", "categorySales", "monthlyRevenue", "products"}, allEntries = true)
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
        String buyerCountry = String.valueOf(req.getOrDefault("buyerCountry", "India")).trim();
        if (buyerCountry.isBlank()) buyerCountry = "India";
        String notes = (String) req.getOrDefault("notes", "");

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Trade Compliance Algorithm (worldwide trade allowed; India <-> Pakistan restricted)
        algorithmService.validateTradeCompliance(product.getOriginCountry(), buyerCountry);

        if (quantity < product.getMoq()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Minimum order quantity (MOQ) for this product is " + product.getMoq() + " " + product.getUnitType() + "."));
        }

        if (product.getStockQuantity() < quantity) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Only " + product.getStockQuantity() + " " + product.getUnitType() + " available."));
        }

        // Dynamic Multi-Tier Bulk Pricing Algorithm
        Map<String, Object> pricing = algorithmService.calculateDynamicBulkPricing(product, quantity);
        boolean isWholesale = (Boolean) pricing.get("isWholesale");
        BigDecimal unitPrice = (BigDecimal) pricing.get("unitPrice");
        BigDecimal totalAmount = (BigDecimal) pricing.get("totalAmount");

        // Generate order ref
        String orderRef = (isWholesale ? "UO-BULK-" : "UO-")
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
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
        order.setBuyerCountry(buyerCountry);
        order.setBuyerRegion(mapCountryToRegion(buyerCountry));
        order.setOrderType(isWholesale ? "WHOLESALE" : "RETAIL");
        order.setNotes(notes);
        order.setStatus("PENDING");
        userOrderRepo.save(order);
        syncOrderToErp(order, user);

        realtimeEventService.publish("USER_ORDER",
                "New " + order.getOrderType() + " order: " + orderRef + " (" + quantity + " " + product.getUnitType() + ", " + buyerCountry + ")",
                user.getFullName());

        return ResponseEntity.ok(ApiResponse.ok("Order placed successfully", Map.of(
                "orderId", order.getId(),
                "orderRef", orderRef,
                "orderType", order.getOrderType(),
                "unitPrice", unitPrice,
                "totalAmount", totalAmount,
                "buyerCountry", buyerCountry,
                "status", order.getStatus()
        )));
    }

    // ── ORDERS: Confirm payment (Razorpay callback) ───────────────────────────
    @PostMapping("/orders/{orderRef}/confirm-payment")
    @CacheEvict(value = {"analyticsSummary", "categorySales", "monthlyRevenue", "products"}, allEntries = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmPayment(
            @PathVariable String orderRef,
            @RequestBody Map<String, String> req) {

        UserOrder order = userOrderRepo.findByOrderRef(orderRef)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderRef));

        order.setRazorpayOrderId(req.getOrDefault("razorpay_order_id", ""));
        order.setRazorpayPaymentId(req.getOrDefault("razorpay_payment_id", ""));
        order.setStatus("PAID");
        userOrderRepo.save(order);
        syncOrderToErp(order, userRepo.findByEmailIgnoreCase(order.getUserEmail()).orElse(null));

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
            m.put("orderType", o.getOrderType());
            m.put("buyerCountry", o.getBuyerCountry());
            m.put("buyerRegion", o.getBuyerRegion());
            m.put("shippingAddress", o.getShippingAddress() != null ? o.getShippingAddress() : "");
            m.put("notes", o.getNotes() != null ? o.getNotes() : "");
            m.put("createdAt", o.getCreatedAt().toString());
            return m;
        }).toList()));
    }

    public static String mapCountryToRegion(String country) {
        if (country == null) return "Global";
        String c = country.trim().toLowerCase();
        if (c.contains("india") || c.contains("pakistan") || c.contains("bangladesh") || c.contains("sri lanka") || c.contains("nepal")) return "South Asia";
        if (c.contains("uae") || c.contains("emirates") || c.contains("saudi") || c.contains("kuwait") || c.contains("qatar") || c.contains("oman") || c.contains("bahrain")) return "Middle East";
        if (c.contains("usa") || c.contains("united states") || c.contains("canada") || c.contains("mexico")) return "North America";
        if (c.contains("uk") || c.contains("united kingdom") || c.contains("germany") || c.contains("france") || c.contains("italy") || c.contains("spain") || c.contains("netherlands")) return "Europe";
        if (c.contains("china") || c.contains("japan") || c.contains("korea") || c.contains("taiwan") || c.contains("hong kong")) return "East Asia";
        if (c.contains("singapore") || c.contains("malaysia") || c.contains("thailand") || c.contains("vietnam") || c.contains("indonesia") || c.contains("philippines")) return "Southeast Asia";
        if (c.contains("australia") || c.contains("new zealand")) return "Oceania";
        if (c.contains("nigeria") || c.contains("kenya") || c.contains("south africa") || c.contains("egypt")) return "Africa";
        if (c.contains("brazil") || c.contains("argentina") || c.contains("chile") || c.contains("colombia")) return "South America";
        return "Global";
    }

    // ── ADMIN: Get all user orders ────────────────────────────────────────────
    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = {"analyticsSummary", "categorySales", "monthlyRevenue", "products"}, allEntries = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllUserOrders() {
        List<UserOrder> orders = userOrderRepo.findAllByOrderByCreatedAtDesc();
        // Auto-sync any store orders not yet mirrored in ERP Sales & Customers
        for (UserOrder o : orders) {
            if (saleRepo.findByOrderNumber(o.getOrderRef()).isEmpty()) {
                syncOrderToErp(o, userRepo.findByEmailIgnoreCase(o.getUserEmail()).orElse(null));
            }
        }
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
    @CacheEvict(value = {"analyticsSummary", "categorySales", "monthlyRevenue", "products"}, allEntries = true)
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> req) {
        UserOrder order = userOrderRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        String nextStatus = req.getOrDefault("status", order.getStatus()).toUpperCase();
        order.setStatus(nextStatus);
        userOrderRepo.save(order);
        syncOrderToErp(order, userRepo.findByEmailIgnoreCase(order.getUserEmail()).orElse(null));
        realtimeEventService.publish("ORDER_STATUS",
                "Order " + order.getOrderRef() + " is now " + nextStatus,
                order.getUserName());
        return ResponseEntity.ok(ApiResponse.ok("Order status updated to " + nextStatus, null));
    }

    /**
     * Mirrors a Customer Portal UserOrder into the Admin ERP Customer + Sale + SaleItem tables
     * so Dashboard KPIs, Sales & Orders, Customers, and Regional Analytics reflect store orders.
     */
    private void syncOrderToErp(UserOrder order, AppUser user) {
        Customer customer = customerRepo.findByEmail(order.getUserEmail()).orElseGet(() -> {
            String full = order.getUserName() != null ? order.getUserName().trim() : "Store Customer";
            String[] parts = full.split("\\s+", 2);
            String first = parts[0];
            String last = parts.length > 1 ? parts[1] : "-";
            String phone = user != null && user.getPhone() != null ? user.getPhone() : "";
            String country = order.getBuyerCountry() != null && !order.getBuyerCountry().isBlank()
                    ? order.getBuyerCountry() : "India";
            return customerRepo.save(new Customer(first, last, order.getUserEmail(), phone, "", country));
        });

        String saleStatus = switch (order.getStatus() != null ? order.getStatus().toUpperCase() : "PENDING") {
            case "PAID", "APPROVED", "ORDERED", "SHIPPED", "DELIVERED", "COMPLETED" -> "COMPLETED";
            case "CANCELLED" -> "CANCELLED";
            default -> "PENDING";
        };

        Sale sale = saleRepo.findByOrderNumber(order.getOrderRef()).orElse(null);
        if (sale == null) {
            sale = new Sale();
            sale.setOrderNumber(order.getOrderRef());
            sale.setCustomer(customer);
            sale.setOrderDate(order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now());
            sale.setTotalAmount(order.getTotalAmount());
            sale.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod() : "DIRECT_QR_SCANNER");
            sale.setStatus(saleStatus);
            sale.setNotes((order.getOrderType() != null ? order.getOrderType() : "RETAIL")
                    + " · " + (order.getBuyerCountry() != null ? order.getBuyerCountry() : "India"));
            SaleItem item = new SaleItem(sale, order.getProduct(), order.getQuantity(), order.getUnitPrice(), order.getTotalAmount());
            sale.addItem(item);
            saleRepo.save(sale);
        } else {
            sale.setStatus(saleStatus);
            saleRepo.save(sale);
        }
    }

    // ── ADMIN: Clear all demo/sample data (requires explicit confirmation) ────
    @PostMapping("/admin/clear-all-data")
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = {"analyticsSummary", "categorySales", "monthlyRevenue", "products", "categories"}, allEntries = true)
    public ResponseEntity<ApiResponse<String>> clearAllData(@RequestBody Map<String, String> req) {
        if (!"CLEAR_ALL_DATA".equals(req.get("confirm"))) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Confirmation token CLEAR_ALL_DATA required."));
        }
        reviewRepo.deleteAll();
        quoteRepo.deleteAll();
        userOrderRepo.deleteAll();
        chatRepo.deleteAll();
        saleRepo.deleteAll();
        productRepo.deleteAll();
        customerRepo.deleteAll();
        realtimeEventService.publish("DATA_CLEARED", "All sample data cleared by Admin", "Admin");
        return ResponseEntity.ok(ApiResponse.ok("All products, orders, quotes, sales & chats cleared. Ready for fresh Admin listings.", "CLEARED"));
    }

    // ── USER: Dashboard summary (own stats) ───────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserDashboard(Authentication auth) {
        String userEmail = auth.getName();
        List<UserOrder> orders = userOrderRepo.findByUserEmailOrderByCreatedAtDesc(userEmail);
        long totalOrders = orders.size();
        long paidOrders = orders.stream().filter(o ->
                "PAID".equals(o.getStatus()) || "APPROVED".equals(o.getStatus())
                || "ORDERED".equals(o.getStatus()) || "SHIPPED".equals(o.getStatus())
                || "DELIVERED".equals(o.getStatus())).count();
        BigDecimal totalSpent = orders.stream()
                .filter(o -> !"CANCELLED".equals(o.getStatus()))
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
