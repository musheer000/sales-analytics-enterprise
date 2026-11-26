package com.salesanalytics.service.impl;

import com.salesanalytics.dto.CategorySalesDTO;
import com.salesanalytics.entity.Sale;
import com.salesanalytics.repository.SaleRepository;
import com.salesanalytics.service.AnalyticsService;
import com.salesanalytics.service.ReportExportService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReportExportServiceImpl implements ReportExportService {

    private final SaleRepository saleRepository;
    private final AnalyticsService analyticsService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ReportExportServiceImpl(SaleRepository saleRepository, AnalyticsService analyticsService) {
        this.saleRepository = saleRepository;
        this.analyticsService = analyticsService;
    }

    @Override
    public byte[] exportSalesToCsv() {
        List<Sale> sales = saleRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDate"));

        StringBuilder csv = new StringBuilder();
        csv.append("Order Number,Customer Name,Customer Email,Order Date,Payment Method,Total Amount,Status,Item Count\n");

        for (Sale s : sales) {
            String customerName = s.getCustomer() != null ? s.getCustomer().getFullName() : "Walk-in";
            String customerEmail = s.getCustomer() != null ? s.getCustomer().getEmail() : "";
            String orderDateStr = s.getOrderDate() != null ? s.getOrderDate().format(DATE_FORMATTER) : "";
            String totalAmountStr = s.getTotalAmount() != null ? s.getTotalAmount().toPlainString() : "0.00";
            int itemCount = s.getItems() != null ? s.getItems().size() : 0;

            csv.append(escapeCsv(s.getOrderNumber())).append(",")
               .append(escapeCsv(customerName)).append(",")
               .append(escapeCsv(customerEmail)).append(",")
               .append(escapeCsv(orderDateStr)).append(",")
               .append(escapeCsv(s.getPaymentMethod())).append(",")
               .append(totalAmountStr).append(",")
               .append(escapeCsv(s.getStatus())).append(",")
               .append(itemCount).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportCategoryRevenueToCsv() {
        List<CategorySalesDTO> list = analyticsService.getSalesByCategory();

        StringBuilder csv = new StringBuilder();
        csv.append("Category ID,Category Name,Total Revenue,Units Sold\n");

        for (CategorySalesDTO dto : list) {
            String revenueStr = dto.getTotalRevenue() != null ? dto.getTotalRevenue().toPlainString() : "0.00";
            long units = dto.getTotalUnitsSold() != null ? dto.getTotalUnitsSold() : 0L;

            csv.append(dto.getCategoryId()).append(",")
               .append(escapeCsv(dto.getCategoryName())).append(",")
               .append(revenueStr).append(",")
               .append(units).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportTopProductsToCsv() {
        List<Map<String, Object>> list = analyticsService.getTopProducts();

        StringBuilder csv = new StringBuilder();
        csv.append("Product Name,Units Sold,Gross Revenue\n");

        for (Map<String, Object> map : list) {
            String name = String.valueOf(map.getOrDefault("productName", ""));
            Object units = map.getOrDefault("unitsSold", 0);
            Object revenue = map.getOrDefault("grossRevenue", "0.00");

            csv.append(escapeCsv(name)).append(",")
               .append(units).append(",")
               .append(revenue).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
