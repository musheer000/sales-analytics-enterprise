package com.salesanalytics.mapper;

import com.salesanalytics.dto.ProductDTO;
import com.salesanalytics.entity.Category;
import com.salesanalytics.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductMapper {

    public ProductDTO toDTO(Product entity) {
        if (entity == null) return null;
        ProductDTO dto = new ProductDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSku(entity.getSku());
        dto.setPrice(entity.getPrice());
        dto.setStockQuantity(entity.getStockQuantity());
        dto.setDescription(entity.getDescription());
        dto.setWholesalePrice(entity.getWholesalePrice());
        dto.setMoq(entity.getMoq());
        dto.setBulkMinQty(entity.getBulkMinQty());
        dto.setUnitType(entity.getUnitType());
        dto.setOriginCountry(entity.getOriginCountry());
        dto.setSellerEmail(entity.getSellerEmail());
        dto.setSellerName(entity.getSellerName());
        dto.setApprovalStatus(entity.getApprovalStatus());
        dto.setNegotiable(entity.isNegotiable());
        dto.setImageUrl(entity.getImageUrl());
        dto.setGradeSpec(entity.getGradeSpec());
        dto.setPackagingInfo(entity.getPackagingInfo());
        dto.setShelfLife(entity.getShelfLife());
        if (entity.getCategory() != null) {
            dto.setCategoryId(entity.getCategory().getId());
            dto.setCategoryName(entity.getCategory().getName());
        }
        return dto;
    }

    public Product toEntity(ProductDTO dto, Category category) {
        if (dto == null) return null;
        Product entity = new Product();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setSku(dto.getSku());
        entity.setPrice(dto.getPrice());
        entity.setStockQuantity(dto.getStockQuantity());
        entity.setDescription(dto.getDescription());
        entity.setCategory(category);
        entity.setWholesalePrice(dto.getWholesalePrice() != null
                ? dto.getWholesalePrice()
                : (dto.getPrice() != null ? dto.getPrice().multiply(new BigDecimal("0.85")) : null));
        entity.setMoq(dto.getMoq() != null ? Math.max(1, dto.getMoq()) : 1);
        entity.setBulkMinQty(dto.getBulkMinQty() != null ? Math.max(1, dto.getBulkMinQty()) : 10);
        entity.setUnitType(dto.getUnitType() != null && !dto.getUnitType().isBlank() ? dto.getUnitType() : "pcs");
        entity.setOriginCountry(dto.getOriginCountry() != null && !dto.getOriginCountry().isBlank() ? dto.getOriginCountry() : "India");
        entity.setSellerEmail(dto.getSellerEmail());
        entity.setSellerName(dto.getSellerName() != null && !dto.getSellerName().isBlank() ? dto.getSellerName() : "Platform Owner");
        entity.setApprovalStatus(dto.getApprovalStatus() != null ? dto.getApprovalStatus() : "APPROVED");
        entity.setNegotiable(dto.isNegotiable());
        entity.setImageUrl(dto.getImageUrl());
        entity.setGradeSpec(dto.getGradeSpec());
        entity.setPackagingInfo(dto.getPackagingInfo());
        entity.setShelfLife(dto.getShelfLife());
        return entity;
    }
}
