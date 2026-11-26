package com.salesanalytics.service.impl;

import com.salesanalytics.dto.AnalyticsSummaryDTO;
import com.salesanalytics.dto.CategorySalesDTO;
import com.salesanalytics.dto.MonthlyRevenueDTO;
import com.salesanalytics.repository.CustomerRepository;
import com.salesanalytics.repository.ProductRepository;
import com.salesanalytics.repository.SaleRepository;
import com.salesanalytics.service.AnalyticsService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public AnalyticsServiceImpl(SaleRepository saleRepository, CustomerRepository customerRepository, ProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Cacheable(value = "analyticsSummary", key = "'kpis'")
    public AnalyticsSummaryDTO getSummary() {
        BigDecimal totalRevenue = saleRepository.calculateTotalRevenue();
        Long totalOrders = saleRepository.countCompletedSales();
        Long totalCustomers = customerRepository.count();
        Long totalProducts = productRepository.count();

        BigDecimal aov = BigDecimal.ZERO;
        if (totalOrders > 0) {
            aov = totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
        }

        return new AnalyticsSummaryDTO(totalRevenue, totalOrders, totalCustomers, totalProducts, aov);
    }

    @Override
    @Cacheable(value = "categorySales", key = "'all'")
    public List<CategorySalesDTO> getSalesByCategory() {
        List<Object[]> rows = saleRepository.findRevenueByCategory();
        List<CategorySalesDTO> results = new ArrayList<>();
        for (Object[] r : rows) {
            Long catId = ((Number) r[0]).longValue();
            String catName = (String) r[1];
            BigDecimal revenue = (BigDecimal) r[2];
            Long units = ((Number) r[3]).longValue();
            results.add(new CategorySalesDTO(catId, catName, revenue, units));
        }
        return results;
    }

    @Override
    @Cacheable(value = "monthlyRevenue", key = "'all'")
    public List<MonthlyRevenueDTO> getMonthlyRevenueTrends() {
        List<Object[]> rows = saleRepository.findMonthlyRevenueTrends();
        List<MonthlyRevenueDTO> results = new ArrayList<>();
        for (Object[] r : rows) {
            String period = (String) r[0];
            BigDecimal revenue = (BigDecimal) r[1];
            Long orders = ((Number) r[2]).longValue();
            results.add(new MonthlyRevenueDTO(period, revenue, orders));
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> getTopProducts() {
        List<Object[]> rows = saleRepository.findTopSellingProducts();
        List<Map<String, Object>> results = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", r[0]);
            map.put("unitsSold", r[1]);
            map.put("revenue", r[2]);
            results.add(map);
        }
        return results;
    }
}
