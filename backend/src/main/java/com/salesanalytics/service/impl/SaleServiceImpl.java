package com.salesanalytics.service.impl;

import com.salesanalytics.dto.CreateSaleRequest;
import com.salesanalytics.dto.PagedResponse;
import com.salesanalytics.dto.SaleDTO;
import com.salesanalytics.entity.Customer;
import com.salesanalytics.entity.Product;
import com.salesanalytics.entity.Sale;
import com.salesanalytics.entity.SaleItem;
import com.salesanalytics.exception.BadRequestException;
import com.salesanalytics.exception.ResourceNotFoundException;
import com.salesanalytics.mapper.SaleMapper;
import com.salesanalytics.repository.CustomerRepository;
import com.salesanalytics.repository.ProductRepository;
import com.salesanalytics.repository.SaleRepository;
import com.salesanalytics.service.SaleService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class SaleServiceImpl implements SaleService {

    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final SaleMapper saleMapper;

    public SaleServiceImpl(SaleRepository saleRepository, CustomerRepository customerRepository, ProductRepository productRepository, SaleMapper saleMapper) {
        this.saleRepository = saleRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.saleMapper = saleMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SaleDTO> filterSalesPaged(String paymentMethod, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<Sale> page = saleRepository.filterSales(paymentMethod, startDate, endDate, pageable);
        return PagedResponse.from(page.map(saleMapper::toDTO));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SaleDTO> getAllSalesPaged(Pageable pageable) {
        Page<Sale> page = saleRepository.findAll(pageable);
        Page<SaleDTO> dtoPage = page.map(saleMapper::toDTO);
        return PagedResponse.from(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleDTO> getAllSales() {
        return saleRepository.findAll().stream().map(saleMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SaleDTO getSaleById(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale/Order not found with ID: " + id));
        return saleMapper.toDTO(sale);
    }

    @Override
    @CacheEvict(value = {"analyticsSummary", "categorySales", "monthlyRevenue"}, allEntries = true)
    public SaleDTO createSale(CreateSaleRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + request.getCustomerId()));

        Sale sale = new Sale();
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        sale.setOrderNumber("ORD-" + datePrefix + "-" + randomSuffix);
        sale.setCustomer(customer);
        sale.setPaymentMethod(request.getPaymentMethod());
        sale.setNotes(request.getNotes());
        sale.setStatus("COMPLETED");

        BigDecimal grandTotal = BigDecimal.ZERO;

        for (CreateSaleRequest.CreateItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + itemReq.getProductId()));

            if (product.getStockQuantity() < itemReq.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product: " + product.getName() + " (Available: " + product.getStockQuantity() + ")");
            }

            product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
            productRepository.save(product);

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            grandTotal = grandTotal.add(subtotal);

            SaleItem item = new SaleItem();
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setSubtotal(subtotal);

            sale.addItem(item);
        }

        sale.setTotalAmount(grandTotal);
        Sale saved = saleRepository.save(sale);
        return saleMapper.toDTO(saved);
    }

    @Override
    @CacheEvict(value = {"analyticsSummary", "categorySales", "monthlyRevenue"}, allEntries = true)
    public void deleteSale(Long id) {
        if (!saleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Sale/Order not found with ID: " + id);
        }
        saleRepository.deleteById(id);
    }
}
