package com.salesanalytics.service;

import com.salesanalytics.dto.CreateSaleRequest;
import com.salesanalytics.dto.PagedResponse;
import com.salesanalytics.dto.SaleDTO;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleService {
    PagedResponse<SaleDTO> filterSalesPaged(String paymentMethod, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    PagedResponse<SaleDTO> getAllSalesPaged(Pageable pageable);
    List<SaleDTO> getAllSales();
    SaleDTO getSaleById(Long id);
    SaleDTO createSale(CreateSaleRequest request);
    void deleteSale(Long id);
}
