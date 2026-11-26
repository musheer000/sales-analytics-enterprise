package com.salesanalytics.mapper;

import com.salesanalytics.dto.CustomerDTO;
import com.salesanalytics.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerDTO toDTO(Customer entity) {
        if (entity == null) return null;
        CustomerDTO dto = new CustomerDTO();
        dto.setId(entity.getId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setCity(entity.getCity());
        dto.setCountry(entity.getCountry());
        return dto;
    }

    public Customer toEntity(CustomerDTO dto) {
        if (dto == null) return null;
        Customer entity = new Customer();
        entity.setId(dto.getId());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setCity(dto.getCity());
        entity.setCountry(dto.getCountry());
        return entity;
    }
}
