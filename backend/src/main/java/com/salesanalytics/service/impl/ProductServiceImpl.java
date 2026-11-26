package com.salesanalytics.service.impl;

import com.salesanalytics.dto.PagedResponse;
import com.salesanalytics.dto.ProductDTO;
import com.salesanalytics.entity.Category;
import com.salesanalytics.entity.Product;
import com.salesanalytics.exception.BadRequestException;
import com.salesanalytics.exception.CategoryNotFoundException;
import com.salesanalytics.exception.ResourceNotFoundException;
import com.salesanalytics.mapper.ProductMapper;
import com.salesanalytics.repository.CategoryRepository;
import com.salesanalytics.repository.ProductRepository;
import com.salesanalytics.service.ProductService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductDTO> searchProductsPaged(Long categoryId, String search, Pageable pageable) {
        Page<Product> page = productRepository.searchProducts(categoryId, search, pageable);
        return PagedResponse.from(page.map(productMapper::toDTO));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> searchProductsList(Long categoryId, String search) {
        return productRepository.searchProductsList(categoryId, search).stream()
                .map(productMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductDTO> getAllProductsPaged(Pageable pageable) {
        Page<Product> page = productRepository.findAll(pageable);
        Page<ProductDTO> dtoPage = page.map(productMapper::toDTO);
        return PagedResponse.from(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductDTO> getProductsByCategoryPaged(Long categoryId, Pageable pageable) {
        Page<Product> page = productRepository.findByCategoryId(categoryId, pageable);
        Page<ProductDTO> dtoPage = page.map(productMapper::toDTO);
        return PagedResponse.from(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'all'")
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream().map(productMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId).stream().map(productMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        return productMapper.toDTO(product);
    }

    @Override
    @CacheEvict(value = {"products", "analyticsSummary", "categorySales"}, allEntries = true)
    public ProductDTO createProduct(ProductDTO dto) {
        if (productRepository.existsBySku(dto.getSku())) {
            throw new BadRequestException("Product with SKU '" + dto.getSku() + "' already exists");
        }
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(dto.getCategoryId()));

        Product product = productMapper.toEntity(dto, category);
        Product saved = productRepository.save(product);
        return productMapper.toDTO(saved);
    }

    @Override
    @CacheEvict(value = {"products", "analyticsSummary", "categorySales"}, allEntries = true)
    public ProductDTO updateProduct(Long id, ProductDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(dto.getCategoryId()));

        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity());
        product.setDescription(dto.getDescription());
        product.setCategory(category);

        Product updated = productRepository.save(product);
        return productMapper.toDTO(updated);
    }

    @Override
    @CacheEvict(value = {"products", "analyticsSummary", "categorySales"}, allEntries = true)
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with ID: " + id);
        }
        productRepository.deleteById(id);
    }
}
