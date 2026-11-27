package com.salesanalytics.service;

import com.salesanalytics.dto.AnalyticsSummaryDTO;
import com.salesanalytics.dto.CategorySalesDTO;
import com.salesanalytics.dto.MonthlyRevenueDTO;

import java.util.List;
import java.util.Map;

public interface AnalyticsService {
    AnalyticsSummaryDTO getSummary();
    List<CategorySalesDTO> getSalesByCategory();
    List<MonthlyRevenueDTO> getMonthlyRevenueTrends();
    List<Map<String, Object>> getTopProducts();
    List<Map<String, Object>> getRegionalSales();
    Map<String, Object> getBehaviorAnalytics();
}
