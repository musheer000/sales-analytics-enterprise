package com.salesanalytics.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lightweight self-service order placed by a USER-role account via the User Portal.
 * Linked to the main sales table via saleId once confirmed.
 */
@Entity
@Table(name = "user_orders", indexes = {
        @Index(name = "idx_uorder_user", columnList = "user_email"),
        @Index(name = "idx_uorder_status", columnList = "status")
})
public class UserOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_ref", nullable = false, unique = true, length = 50)
    private String orderRef; // e.g. UO-20261127-0001

    @Column(name = "user_email", nullable = false, length = 150)
    private String userEmail;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod; // RAZORPAY / RAZORPAY_QR / COD

    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Column(nullable = false, length = 30)
    private String status = "PENDING"; // PENDING / PAID / CANCELLED / DELIVERED

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "buyer_country", length = 100)
    private String buyerCountry = "India";

    @Column(name = "buyer_region", length = 100)
    private String buyerRegion = "South Asia";

    @Column(name = "order_type", length = 20)
    private String orderType = "RETAIL"; // RETAIL or WHOLESALE

    @Column(name = "quote_ref", length = 50)
    private String quoteRef;

    @Column(length = 255)
    private String notes;

    @Column(name = "linked_sale_id")
    private Long linkedSaleId; // set after admin approves / auto-syncs

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UserOrder() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getOrderRef() { return orderRef; }
    public void setOrderRef(String orderRef) { this.orderRef = orderRef; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getLinkedSaleId() { return linkedSaleId; }
    public void setLinkedSaleId(Long linkedSaleId) { this.linkedSaleId = linkedSaleId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public String getBuyerCountry() { return buyerCountry != null ? buyerCountry : "India"; }
    public void setBuyerCountry(String buyerCountry) { this.buyerCountry = buyerCountry; }
    public String getBuyerRegion() { return buyerRegion != null ? buyerRegion : "South Asia"; }
    public void setBuyerRegion(String buyerRegion) { this.buyerRegion = buyerRegion; }
    public String getOrderType() { return orderType != null ? orderType : "RETAIL"; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getQuoteRef() { return quoteRef; }
    public void setQuoteRef(String quoteRef) { this.quoteRef = quoteRef; }
}
