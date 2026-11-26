package com.salesanalytics.service;

import com.salesanalytics.dto.AnalyticsSummaryDTO;
import com.salesanalytics.repository.CustomerRepository;
import com.salesanalytics.repository.ProductRepository;
import com.salesanalytics.repository.SaleRepository;
import com.salesanalytics.service.impl.AnalyticsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Test
    @DisplayName("getSummary - Should accurately compute KPIs and Average Order Value")
    void testGetSummary() {
        when(saleRepository.calculateTotalRevenue()).thenReturn(new BigDecimal("3000.00"));
        when(saleRepository.countCompletedSales()).thenReturn(6L);
        when(customerRepository.count()).thenReturn(10L);
        when(productRepository.count()).thenReturn(25L);

        AnalyticsSummaryDTO summary = analyticsService.getSummary();

        assertNotNull(summary);
        assertEquals(new BigDecimal("3000.00"), summary.getTotalRevenue());
        assertEquals(6L, summary.getTotalOrders());
        assertEquals(10L, summary.getTotalCustomers());
        assertEquals(25L, summary.getTotalProducts());
        // 3000 / 6 = 500.00
        assertEquals(new BigDecimal("500.00"), summary.getAverageOrderValue());
    }

    @Test
    @DisplayName("getSummary - Should handle zero orders gracefully without division error")
    void testGetSummaryZeroOrders() {
        when(saleRepository.calculateTotalRevenue()).thenReturn(BigDecimal.ZERO);
        when(saleRepository.countCompletedSales()).thenReturn(0L);
        when(customerRepository.count()).thenReturn(2L);
        when(productRepository.count()).thenReturn(5L);

        AnalyticsSummaryDTO summary = analyticsService.getSummary();

        assertNotNull(summary);
        assertEquals(BigDecimal.ZERO, summary.getTotalRevenue());
        assertEquals(0L, summary.getTotalOrders());
        assertEquals(BigDecimal.ZERO, summary.getAverageOrderValue());
    }
}
