package com.salesanalytics.controller;

import com.salesanalytics.dto.AnalyticsSummaryDTO;
import com.salesanalytics.dto.ApiResponse;
import com.salesanalytics.dto.CategorySalesDTO;
import com.salesanalytics.dto.MonthlyRevenueDTO;
import com.salesanalytics.service.AnalyticsService;
import com.salesanalytics.service.ReportExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ReportExportService reportExportService;

    public AnalyticsController(AnalyticsService analyticsService, ReportExportService reportExportService) {
        this.analyticsService = analyticsService;
        this.reportExportService = reportExportService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AnalyticsSummaryDTO>> getSummary() {
        AnalyticsSummaryDTO summary = analyticsService.getSummary();
        return ResponseEntity.ok(ApiResponse.ok("Analytics summary retrieved successfully", summary));
    }

    @GetMapping("/category-sales")
    public ResponseEntity<ApiResponse<List<CategorySalesDTO>>> getCategorySales() {
        List<CategorySalesDTO> list = analyticsService.getSalesByCategory();
        return ResponseEntity.ok(ApiResponse.ok("Category sales data retrieved successfully", list));
    }

    @GetMapping("/monthly-revenue")
    public ResponseEntity<ApiResponse<List<MonthlyRevenueDTO>>> getMonthlyRevenue() {
        List<MonthlyRevenueDTO> list = analyticsService.getMonthlyRevenueTrends();
        return ResponseEntity.ok(ApiResponse.ok("Monthly revenue data retrieved successfully", list));
    }

    @GetMapping("/top-products")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopProducts() {
        List<Map<String, Object>> list = analyticsService.getTopProducts();
        return ResponseEntity.ok(ApiResponse.ok("Top products retrieved successfully", list));
    }

    @GetMapping(value = "/export/sales-csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportSalesCsv() {
        byte[] data = reportExportService.exportSalesToCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sales_orders_report.csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(data);
    }

    @GetMapping(value = "/export/category-revenue-csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCategoryRevenueCsv() {
        byte[] data = reportExportService.exportCategoryRevenueToCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"category_revenue_report.csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(data);
    }

    @GetMapping(value = "/export/top-products-csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportTopProductsCsv() {
        byte[] data = reportExportService.exportTopProductsToCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"top_products_report.csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(data);
    }
}
