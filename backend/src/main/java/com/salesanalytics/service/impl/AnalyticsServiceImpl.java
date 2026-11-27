package com.salesanalytics.service.impl;

import com.salesanalytics.controller.UserPortalController;
import com.salesanalytics.dto.AnalyticsSummaryDTO;
import com.salesanalytics.dto.CategorySalesDTO;
import com.salesanalytics.dto.MonthlyRevenueDTO;
import com.salesanalytics.repository.CustomerRepository;
import com.salesanalytics.repository.ProductRepository;
import com.salesanalytics.repository.SaleRepository;
import com.salesanalytics.repository.UserOrderRepository;
import com.salesanalytics.repository.WholesaleQuoteRepository;
import com.salesanalytics.service.AnalyticsService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UserOrderRepository userOrderRepository;
    private final WholesaleQuoteRepository quoteRepository;

    public AnalyticsServiceImpl(SaleRepository saleRepository,
                                CustomerRepository customerRepository,
                                ProductRepository productRepository,
                                UserOrderRepository userOrderRepository,
                                WholesaleQuoteRepository quoteRepository) {
        this.saleRepository = saleRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.userOrderRepository = userOrderRepository;
        this.quoteRepository = quoteRepository;
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

    @Override
    public List<Map<String, Object>> getRegionalSales() {
        Map<String, Map<String, Object>> byCountry = new LinkedHashMap<>();

        // 1. Aggregate ERP Sales by Customer Country
        for (Object[] r : saleRepository.findRevenueByCountry()) {
            String country = String.valueOf(r[0]);
            long orders = ((Number) r[1]).longValue();
            BigDecimal rev = (BigDecimal) r[2];
            String region = UserPortalController.mapCountryToRegion(country);
            Map<String, Object> entry = byCountry.computeIfAbsent(country, k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("country", country);
                m.put("region", region);
                m.put("orders", 0L);
                m.put("units", 0L);
                m.put("revenue", BigDecimal.ZERO);
                return m;
            });
            entry.put("orders", ((Long) entry.get("orders")) + orders);
            entry.put("revenue", ((BigDecimal) entry.get("revenue")).add(rev));
        }

        // 2. Merge Portal UserOrders (Retail + Wholesale) by Buyer Country
        for (Object[] r : userOrderRepository.findUserOrderStatsByCountry()) {
            String country = String.valueOf(r[0]);
            String region = String.valueOf(r[1]);
            long orders = ((Number) r[2]).longValue();
            long units = ((Number) r[3]).longValue();
            BigDecimal rev = (BigDecimal) r[4];
            Map<String, Object> entry = byCountry.computeIfAbsent(country, k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("country", country);
                m.put("region", region);
                m.put("orders", 0L);
                m.put("units", 0L);
                m.put("revenue", BigDecimal.ZERO);
                return m;
            });
            entry.put("orders", ((Long) entry.get("orders")) + orders);
            entry.put("units", ((Long) entry.get("units")) + units);
            entry.put("revenue", ((BigDecimal) entry.get("revenue")).add(rev));
        }

        List<Map<String, Object>> sorted = new ArrayList<>(byCountry.values());
        sorted.sort((a, b) -> ((BigDecimal) b.get("revenue")).compareTo((BigDecimal) a.get("revenue")));
        return sorted;
    }

    @Override
    public Map<String, Object> getBehaviorAnalytics() {
        long wholesaleOrders = 0;
        long retailOrders = 0;
        long wholesaleUnits = 0;
        long retailUnits = 0;
        BigDecimal wholesaleRevenue = BigDecimal.ZERO;
        BigDecimal retailRevenue = BigDecimal.ZERO;

        for (Object[] r : userOrderRepository.findUserOrderStatsByType()) {
            String type = String.valueOf(r[0]);
            long cnt = ((Number) r[1]).longValue();
            long units = ((Number) r[2]).longValue();
            BigDecimal rev = (BigDecimal) r[3];
            if ("WHOLESALE".equalsIgnoreCase(type)) {
                wholesaleOrders += cnt;
                wholesaleUnits += units;
                wholesaleRevenue = wholesaleRevenue.add(rev);
            } else {
                retailOrders += cnt;
                retailUnits += units;
                retailRevenue = retailRevenue.add(rev);
            }
        }

        long totalQuotes = quoteRepository.count();
        long acceptedQuotes = quoteRepository.countByStatus("ACCEPTED") + quoteRepository.countByStatus("ORDERED");
        long pendingApprovals = productRepository.countByApprovalStatus("PENDING");

        Map<String, Object> behavior = new HashMap<>();
        behavior.put("wholesaleOrders", wholesaleOrders);
        behavior.put("retailOrders", retailOrders);
        behavior.put("wholesaleUnits", wholesaleUnits);
        behavior.put("retailUnits", retailUnits);
        behavior.put("wholesaleRevenue", wholesaleRevenue);
        behavior.put("retailRevenue", retailRevenue);
        behavior.put("totalQuotes", totalQuotes);
        behavior.put("acceptedQuotes", acceptedQuotes);
        behavior.put("pendingProductApprovals", pendingApprovals);
        behavior.put("topProducts", getTopProducts());
        behavior.put("regionalSales", getRegionalSales());
        return behavior;
    }
}
