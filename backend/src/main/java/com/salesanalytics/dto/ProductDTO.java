package com.salesanalytics.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ProductDTO {

    private Long id;

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "SKU is required")
    private String sku;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private String categoryName;
    private String description;

    // Bulk Wholesale & Full Product Specs
    private BigDecimal wholesalePrice;
    private Integer moq = 1;
    private Integer bulkMinQty = 10;
    private String unitType = "pcs";
    private String originCountry = "India";
    private String sellerEmail;
    private String sellerName;
    private String approvalStatus = "APPROVED";
    private boolean negotiable = true;
    private String imageUrl;
    private String gradeSpec;
    private String packagingInfo;
    private String shelfLife;

    public ProductDTO() {}

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

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getWholesalePrice() { return wholesalePrice; }
    public void setWholesalePrice(BigDecimal wholesalePrice) { this.wholesalePrice = wholesalePrice; }

    public Integer getMoq() { return moq; }
    public void setMoq(Integer moq) { this.moq = moq; }

    public Integer getBulkMinQty() { return bulkMinQty; }
    public void setBulkMinQty(Integer bulkMinQty) { this.bulkMinQty = bulkMinQty; }

    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }

    public String getOriginCountry() { return originCountry; }
    public void setOriginCountry(String originCountry) { this.originCountry = originCountry; }

    public String getSellerEmail() { return sellerEmail; }
    public void setSellerEmail(String sellerEmail) { this.sellerEmail = sellerEmail; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

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
