package com.salesanalytics.service;

import com.salesanalytics.dto.CustomerDTO;
import com.salesanalytics.dto.CustomerSummaryDTO;
import java.util.List;

public interface CustomerService {
    List<CustomerDTO> getAllCustomers();
    CustomerDTO getCustomerById(Long id);
    CustomerDTO createCustomer(CustomerDTO customerDTO);
    CustomerDTO updateCustomer(Long id, CustomerDTO customerDTO);
    void deleteCustomer(Long id);
    List<CustomerSummaryDTO> getCustomersWithStats();
}
