package com.salesanalytics.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ── B2B Wholesale & Global Marketplace Fields ─────────────────────────────
    @Column(name = "moq", nullable = false)
    private Integer moq = 1; // Minimum Order Quantity for bulk/wholesale

    @Column(name = "wholesale_price", precision = 10, scale = 2)
    private BigDecimal wholesalePrice; // Discounted unit price when qty >= bulkMinQty

    @Column(name = "bulk_min_qty")
    private Integer bulkMinQty = 10;

    @Column(name = "unit_type", length = 30)
    private String unitType = "pcs"; // pcs, boxes, kg, cartons, sets

    @Column(name = "origin_country", length = 100)
    private String originCountry = "India";

    @Column(name = "seller_email", length = 150)
    private String sellerEmail; // null = platform/admin owned

    @Column(name = "seller_name", length = 120)
    private String sellerName = "Platform Direct";

    @Column(name = "approval_status", nullable = false, length = 20)
    private String approvalStatus = "APPROVED"; // PENDING, APPROVED, REJECTED

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "negotiable", nullable = false)
    private boolean negotiable = true;

    @Column(name = "image_url", columnDefinition = "LONGTEXT")
    private String imageUrl; // Product photo URL or base64 data URI

    @Column(name = "grade_spec", length = 120)
    private String gradeSpec; // e.g. Export Grade A, Organic Certified

    @Column(name = "packaging_info", length = 150)
    private String packagingInfo; // e.g. 50kg Jute Bags, Vacuum Sealed

    @Column(name = "shelf_life", length = 80)
    private String shelfLife; // e.g. 12 Months, Fresh 7 Days

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Product() {}

    public Product(String name, String sku, BigDecimal price, Integer stockQuantity, Category category, String description) {
        this.name = name;
        this.sku = sku;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.category = category;
        this.description = description;
    }

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
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Integer getMoq() { return moq != null ? moq : 1; }
    public void setMoq(Integer moq) { this.moq = moq; }

    public BigDecimal getWholesalePrice() { return wholesalePrice; }
    public void setWholesalePrice(BigDecimal wholesalePrice) { this.wholesalePrice = wholesalePrice; }

    public Integer getBulkMinQty() { return bulkMinQty != null ? bulkMinQty : 10; }
    public void setBulkMinQty(Integer bulkMinQty) { this.bulkMinQty = bulkMinQty; }

    public String getUnitType() { return unitType != null ? unitType : "pcs"; }
    public void setUnitType(String unitType) { this.unitType = unitType; }

    public String getOriginCountry() { return originCountry != null ? originCountry : "India"; }
    public void setOriginCountry(String originCountry) { this.originCountry = originCountry; }

    public String getSellerEmail() { return sellerEmail; }
    public void setSellerEmail(String sellerEmail) { this.sellerEmail = sellerEmail; }

    public String getSellerName() { return sellerName != null ? sellerName : "Platform Direct"; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getApprovalStatus() { return approvalStatus != null ? approvalStatus : "APPROVED"; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public boolean isNegotiable() { return negotiable; }
    public void setNegotiable(boolean negotiable) { this.negotiable = negotiable; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getGradeSpec() { return gradeSpec; }
    public void setGradeSpec(String gradeSpec) { this.gradeSpec = gradeSpec; }

    public String getPackagingInfo() { return packagingInfo; }
    public void setPackagingInfo(String packagingInfo) { this.packagingInfo = packagingInfo; }

    public String getShelfLife() { return shelfLife; }
    public void setShelfLife(String shelfLife) { this.shelfLife = shelfLife; }
}
