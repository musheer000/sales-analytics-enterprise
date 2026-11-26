package com.salesanalytics.service;

import com.salesanalytics.dto.PagedResponse;
import com.salesanalytics.dto.ProductDTO;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ProductService {
    PagedResponse<ProductDTO> searchProductsPaged(Long categoryId, String search, Pageable pageable);
    List<ProductDTO> searchProductsList(Long categoryId, String search);
    PagedResponse<ProductDTO> getAllProductsPaged(Pageable pageable);
    PagedResponse<ProductDTO> getProductsByCategoryPaged(Long categoryId, Pageable pageable);
    List<ProductDTO> getAllProducts();
    List<ProductDTO> getProductsByCategory(Long categoryId);
    ProductDTO getProductById(Long id);
    ProductDTO createProduct(ProductDTO productDTO);
    ProductDTO updateProduct(Long id, ProductDTO productDTO);
    void deleteProduct(Long id);
}
