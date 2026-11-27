package com.salesanalytics.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * B2B Wholesale / Bulk Request-for-Quote (RFQ) & Price Negotiation entity.
 * Allows buyers from any country to negotiate bulk quantities, quality specs,
 * and custom unit prices directly with a seller or the platform admin.
 */
@Entity
@Table(name = "wholesale_quotes", indexes = {
        @Index(name = "idx_wq_buyer", columnList = "buyer_email"),
        @Index(name = "idx_wq_seller", columnList = "seller_email"),
        @Index(name = "idx_wq_status", columnList = "status")
})
public class WholesaleQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quote_ref", nullable = false, unique = true, length = 50)
    private String quoteRef; // e.g. RFQ-20261127-0001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "buyer_email", nullable = false, length = 150)
    private String buyerEmail;

    @Column(name = "buyer_name", nullable = false, length = 120)
    private String buyerName;

    @Column(name = "buyer_country", nullable = false, length = 100)
    private String buyerCountry = "India";

    @Column(name = "seller_email", length = 150)
    private String sellerEmail; // null = platform/admin owned

    @Column(name = "seller_name", length = 120)
    private String sellerName = "Platform Direct";

    @Column(name = "requested_qty", nullable = false)
    private int requestedQty;

    @Column(name = "original_unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalUnitPrice;

    @Column(name = "proposed_unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal proposedUnitPrice;

    @Column(name = "counter_unit_price", precision = 10, scale = 2)
    private BigDecimal counterUnitPrice;

    @Column(name = "agreed_unit_price", precision = 10, scale = 2)
    private BigDecimal agreedUnitPrice;

    @Column(nullable = false, length = 25)
    private String status = "PENDING"; // PENDING, COUNTERED, ACCEPTED, REJECTED, ORDERED

    @Column(name = "quality_notes", columnDefinition = "TEXT")
    private String qualityNotes;

    @Column(name = "seller_response", columnDefinition = "TEXT")
    private String sellerResponse;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public WholesaleQuote() {}

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
    public String getQuoteRef() { return quoteRef; }
    public void setQuoteRef(String quoteRef) { this.quoteRef = quoteRef; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String buyerEmail) { this.buyerEmail = buyerEmail; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public String getBuyerCountry() { return buyerCountry; }
    public void setBuyerCountry(String buyerCountry) { this.buyerCountry = buyerCountry; }
    public String getSellerEmail() { return sellerEmail; }
    public void setSellerEmail(String sellerEmail) { this.sellerEmail = sellerEmail; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public int getRequestedQty() { return requestedQty; }
    public void setRequestedQty(int requestedQty) { this.requestedQty = requestedQty; }
    public BigDecimal getOriginalUnitPrice() { return originalUnitPrice; }
    public void setOriginalUnitPrice(BigDecimal originalUnitPrice) { this.originalUnitPrice = originalUnitPrice; }
    public BigDecimal getProposedUnitPrice() { return proposedUnitPrice; }
    public void setProposedUnitPrice(BigDecimal proposedUnitPrice) { this.proposedUnitPrice = proposedUnitPrice; }
    public BigDecimal getCounterUnitPrice() { return counterUnitPrice; }
    public void setCounterUnitPrice(BigDecimal counterUnitPrice) { this.counterUnitPrice = counterUnitPrice; }
    public BigDecimal getAgreedUnitPrice() { return agreedUnitPrice; }
    public void setAgreedUnitPrice(BigDecimal agreedUnitPrice) { this.agreedUnitPrice = agreedUnitPrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getQualityNotes() { return qualityNotes; }
    public void setQualityNotes(String qualityNotes) { this.qualityNotes = qualityNotes; }
    public String getSellerResponse() { return sellerResponse; }
    public void setSellerResponse(String sellerResponse) { this.sellerResponse = sellerResponse; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
