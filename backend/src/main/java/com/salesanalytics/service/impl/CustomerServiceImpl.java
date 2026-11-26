package com.salesanalytics.service.impl;

import com.salesanalytics.dto.CustomerDTO;
import com.salesanalytics.dto.CustomerSummaryDTO;
import com.salesanalytics.entity.Customer;
import com.salesanalytics.exception.BadRequestException;
import com.salesanalytics.exception.ResourceNotFoundException;
import com.salesanalytics.mapper.CustomerMapper;
import com.salesanalytics.repository.CustomerRepository;
import com.salesanalytics.service.CustomerService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerServiceImpl(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream().map(customerMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
        return customerMapper.toDTO(customer);
    }

    @Override
    @CacheEvict(value = "analyticsSummary", allEntries = true)
    public CustomerDTO createCustomer(CustomerDTO dto) {
        if (customerRepository.existsByEmail(dto.getEmail())) {
            throw new BadRequestException("Customer with email '" + dto.getEmail() + "' already exists");
        }
        Customer customer = customerMapper.toEntity(dto);
        Customer saved = customerRepository.save(customer);
        return customerMapper.toDTO(saved);
    }

    @Override
    public CustomerDTO updateCustomer(Long id, CustomerDTO dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));

        customer.setFirstName(dto.getFirstName());
        customer.setLastName(dto.getLastName());
        customer.setPhone(dto.getPhone());
        customer.setCity(dto.getCity());
        customer.setCountry(dto.getCountry());

        Customer updated = customerRepository.save(customer);
        return customerMapper.toDTO(updated);
    }

    @Override
    @CacheEvict(value = "analyticsSummary", allEntries = true)
    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Customer not found with ID: " + id);
        }
        customerRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerSummaryDTO> getCustomersWithStats() {
        List<Object[]> rows = customerRepository.findCustomersWithStats();
        return rows.stream().map(row -> {
            Long id = ((Number) row[0]).longValue();
            String firstName = (String) row[1];
            String lastName = (String) row[2];
            String email = (String) row[3];
            String phone = (String) row[4];
            String city = (String) row[5];
            String country = (String) row[6];
            Long totalOrders = row[7] != null ? ((Number) row[7]).longValue() : 0L;
            BigDecimal totalSpent = BigDecimal.ZERO;
            if (row[8] != null) {
                if (row[8] instanceof BigDecimal) {
                    totalSpent = (BigDecimal) row[8];
                } else {
                    totalSpent = BigDecimal.valueOf(((Number) row[8]).doubleValue());
                }
            }
            LocalDateTime lastOrderDate = (LocalDateTime) row[9];
            return new CustomerSummaryDTO(id, firstName, lastName, email, phone, city, country, totalOrders, totalSpent, lastOrderDate);
        }).collect(Collectors.toList());
    }
}
