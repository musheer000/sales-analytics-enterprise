package com.salesanalytics.dto;

import java.math.BigDecimal;

public class MonthlyRevenueDTO {

    private String period;
    private BigDecimal revenue;
    private Long orderCount;

    public MonthlyRevenueDTO() {}

    public MonthlyRevenueDTO(String period, BigDecimal revenue, Long orderCount) {
        this.period = period;
        this.revenue = revenue;
        this.orderCount = orderCount;
    }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

    public Long getOrderCount() { return orderCount; }
    public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }
}
