package com.salesanalytics.service;

import com.salesanalytics.dto.CreateSaleRequest;
import com.salesanalytics.dto.SaleDTO;
import com.salesanalytics.entity.Category;
import com.salesanalytics.entity.Customer;
import com.salesanalytics.entity.Product;
import com.salesanalytics.entity.Sale;
import com.salesanalytics.exception.BadRequestException;
import com.salesanalytics.exception.ResourceNotFoundException;
import com.salesanalytics.mapper.SaleMapper;
import com.salesanalytics.repository.CustomerRepository;
import com.salesanalytics.repository.ProductRepository;
import com.salesanalytics.repository.SaleRepository;
import com.salesanalytics.service.impl.SaleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Spy
    private SaleMapper saleMapper = new SaleMapper();

    @InjectMocks
    private SaleServiceImpl saleService;

    private Customer testCustomer;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer("Alexander", "Wright", "alex@example.com", "555-0101", "San Francisco", "USA");
        testCustomer.setId(1L);

        Category category = new Category(1L, "Electronics", "Devices");
        testProduct = new Product("4K Monitor", "ELEC-01", new BigDecimal("300.00"), 10, category, "Display");
        testProduct.setId(100L);
    }

    @Test
    @DisplayName("createSale - Should deduct inventory and save sale successfully")
    void testCreateSaleSuccess() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale s = invocation.getArgument(0);
            s.setId(50L);
            return s;
        });

        CreateSaleRequest request = new CreateSaleRequest();
        request.setCustomerId(1L);
        request.setPaymentMethod("CREDIT_CARD");
        request.setNotes("Office order");

        CreateSaleRequest.CreateItemRequest item = new CreateSaleRequest.CreateItemRequest();
        item.setProductId(100L);
        item.setQuantity(2);
        request.setItems(List.of(item));

        SaleDTO result = saleService.createSale(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("600.00"), result.getTotalAmount());
        assertEquals("CREDIT_CARD", result.getPaymentMethod());

        // Verify stock was reduced from 10 to 8
        assertEquals(8, testProduct.getStockQuantity());
        verify(productRepository, times(1)).save(testProduct);
        verify(saleRepository, times(1)).save(any(Sale.class));
    }

    @Test
    @DisplayName("createSale - Should throw BadRequestException when requested quantity exceeds stock")
    void testCreateSaleInsufficientStock() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

        CreateSaleRequest request = new CreateSaleRequest();
        request.setCustomerId(1L);
        request.setPaymentMethod("CREDIT_CARD");

        CreateSaleRequest.CreateItemRequest item = new CreateSaleRequest.CreateItemRequest();
        item.setProductId(100L);
        item.setQuantity(25); // Exceeds stock of 10
        request.setItems(List.of(item));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> saleService.createSale(request));
        assertTrue(ex.getMessage().contains("Insufficient stock"));

        // Verify sale was never saved
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    @DisplayName("createSale - Should throw ResourceNotFoundException when customer does not exist")
    void testCreateSaleCustomerNotFound() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        CreateSaleRequest request = new CreateSaleRequest();
        request.setCustomerId(999L);
        request.setPaymentMethod("PAYPAL");

        CreateSaleRequest.CreateItemRequest item = new CreateSaleRequest.CreateItemRequest();
        item.setProductId(100L);
        item.setQuantity(1);
        request.setItems(List.of(item));

        assertThrows(ResourceNotFoundException.class, () -> saleService.createSale(request));
        verify(saleRepository, never()).save(any(Sale.class));
    }
}
