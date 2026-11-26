package com.salesanalytics.mapper;

import com.salesanalytics.dto.SaleDTO;
import com.salesanalytics.dto.SaleItemDTO;
import com.salesanalytics.entity.Sale;
import com.salesanalytics.entity.SaleItem;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class SaleMapper {

    public SaleDTO toDTO(Sale entity) {
        if (entity == null) return null;
        SaleDTO dto = new SaleDTO();
        dto.setId(entity.getId());
        dto.setOrderNumber(entity.getOrderNumber());
        dto.setOrderDate(entity.getOrderDate());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setStatus(entity.getStatus());
        dto.setNotes(entity.getNotes());

        if (entity.getCustomer() != null) {
            dto.setCustomerId(entity.getCustomer().getId());
            dto.setCustomerName(entity.getCustomer().getFullName());
            dto.setCustomerEmail(entity.getCustomer().getEmail());
        }

        if (entity.getItems() != null) {
            dto.setItems(entity.getItems().stream().map(this::toItemDTO).collect(Collectors.toList()));
        }

        return dto;
    }

    public SaleItemDTO toItemDTO(SaleItem item) {
        if (item == null) return null;
        SaleItemDTO dto = new SaleItemDTO();
        dto.setId(item.getId());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setSubtotal(item.getSubtotal());

        if (item.getProduct() != null) {
            dto.setProductId(item.getProduct().getId());
            dto.setProductName(item.getProduct().getName());
            dto.setProductSku(item.getProduct().getSku());
        }

        return dto;
    }
}
