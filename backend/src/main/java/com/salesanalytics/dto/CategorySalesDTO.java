package com.salesanalytics.dto;

import java.math.BigDecimal;

public class CategorySalesDTO {

    private Long categoryId;
    private String categoryName;
    private BigDecimal totalRevenue;
    private Long totalUnitsSold;

    public CategorySalesDTO() {}

    public CategorySalesDTO(Long categoryId, String categoryName, BigDecimal totalRevenue, Long totalUnitsSold) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.totalRevenue = totalRevenue;
        this.totalUnitsSold = totalUnitsSold;
    }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public Long getTotalUnitsSold() { return totalUnitsSold; }
    public void setTotalUnitsSold(Long totalUnitsSold) { this.totalUnitsSold = totalUnitsSold; }
}
