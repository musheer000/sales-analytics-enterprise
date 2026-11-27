package com.salesanalytics.repository;

import com.salesanalytics.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    Optional<Product> findBySku(String sku);
    boolean existsBySku(String sku);

    // Eagerly fetch category to avoid LazyInitializationException on user portal
    @Query("SELECT p FROM Product p JOIN FETCH p.category")
    List<Product> findAllWithCategory();

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.approvalStatus = 'APPROVED' ORDER BY p.id DESC")
    List<Product> findApprovedWithCategory();

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.approvalStatus = :status ORDER BY p.createdAt DESC")
    List<Product> findByApprovalStatusWithCategory(@Param("status") String status);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE LOWER(p.sellerEmail) = LOWER(:email) ORDER BY p.createdAt DESC")
    List<Product> findBySellerEmailWithCategory(@Param("email") String email);

    long countByApprovalStatus(String approvalStatus);

    @Query("SELECT p FROM Product p WHERE (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:search IS NULL OR :search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchProducts(@Param("categoryId") Long categoryId, @Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:search IS NULL OR :search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Product> searchProductsList(@Param("categoryId") Long categoryId, @Param("search") String search);
}
