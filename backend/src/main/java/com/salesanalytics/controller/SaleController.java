package com.salesanalytics.controller;

import com.salesanalytics.dto.ApiResponse;
import com.salesanalytics.dto.CreateSaleRequest;
import com.salesanalytics.dto.PagedResponse;
import com.salesanalytics.dto.SaleDTO;
import com.salesanalytics.service.RealtimeEventService;
import com.salesanalytics.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "*")
public class SaleController {

    private final SaleService saleService;
    private final RealtimeEventService realtimeEventService;

    public SaleController(SaleService saleService, RealtimeEventService realtimeEventService) {
        this.saleService = saleService;
        this.realtimeEventService = realtimeEventService;
    }

    /**
     * GET /api/sales?page=0&size=10&sort=orderDate&direction=desc&paymentMethod=CREDIT_CARD&startDate=...&endDate=...
     * Returns a paged result when ?page is supplied; full list otherwise.
     */
    @GetMapping
    public ResponseEntity<?> getAllSales(
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderDate") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        LocalDateTime start = null;
        if (startDate != null && !startDate.isBlank()) {
            try {
                start = startDate.contains("T") ? LocalDateTime.parse(startDate) : LocalDate.parse(startDate).atStartOfDay();
            } catch (Exception ignored) {}
        }

        LocalDateTime end = null;
        if (endDate != null && !endDate.isBlank()) {
            try {
                end = endDate.contains("T") ? LocalDateTime.parse(endDate) : LocalDate.parse(endDate).atTime(23, 59, 59);
            } catch (Exception ignored) {}
        }

        if (page != null) {
            Sort.Direction dir = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort));
            PagedResponse<SaleDTO> paged = saleService.filterSalesPaged(paymentMethod, start, end, pageable);
            return ResponseEntity.ok(ApiResponse.ok("Sales orders retrieved successfully", paged));
        }

        List<SaleDTO> sales = saleService.getAllSales();
        return ResponseEntity.ok(ApiResponse.ok("Sales orders retrieved successfully", sales));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleDTO>> getSaleById(@PathVariable Long id) {
        SaleDTO sale = saleService.getSaleById(id);
        return ResponseEntity.ok(ApiResponse.ok("Sale retrieved successfully", sale));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SaleDTO>> createSale(@Valid @RequestBody CreateSaleRequest request) {
        SaleDTO created = saleService.createSale(request);
        realtimeEventService.publish(
                "SALE_CREATED",
                "Order " + created.getOrderNumber() + " ($" + created.getTotalAmount() + " USD • " + created.getPaymentMethod() + ")",
                created.getCustomerName()
        );
        return new ResponseEntity<>(ApiResponse.ok("Sale created successfully", created), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSale(@PathVariable Long id) {
        saleService.deleteSale(id);
        realtimeEventService.publish("SALE_DELETED", "Order #" + id + " removed", "admin");
        return ResponseEntity.ok(ApiResponse.ok("Sale deleted successfully", null));
    }
}
