package com.salesanalytics.repository;

import com.salesanalytics.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findByOrderNumber(String orderNumber);

    Page<Sale> findAll(Pageable pageable);

    @Query("SELECT s FROM Sale s WHERE (:paymentMethod IS NULL OR :paymentMethod = '' OR s.paymentMethod = :paymentMethod) " +
           "AND (:startDate IS NULL OR s.orderDate >= :startDate) " +
           "AND (:endDate IS NULL OR s.orderDate <= :endDate)")
    Page<Sale> filterSales(@Param("paymentMethod") String paymentMethod,
                           @Param("startDate") LocalDateTime startDate,
                           @Param("endDate") LocalDateTime endDate,
                           Pageable pageable);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.status = 'COMPLETED'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.status = 'COMPLETED'")
    Long countCompletedSales();

    @Query("SELECT c.id, c.name, COALESCE(SUM(si.subtotal), 0), COALESCE(SUM(si.quantity), 0) " +
           "FROM Category c " +
           "JOIN c.products p " +
           "JOIN SaleItem si ON si.product.id = p.id " +
           "JOIN si.sale s " +
           "WHERE s.status = 'COMPLETED' " +
           "GROUP BY c.id, c.name " +
           "ORDER BY SUM(si.subtotal) DESC")
    List<Object[]> findRevenueByCategory();

    @Query("SELECT FUNCTION('DATE_FORMAT', s.orderDate, '%Y-%m'), SUM(s.totalAmount), COUNT(s) " +
           "FROM Sale s " +
           "WHERE s.status = 'COMPLETED' " +
           "GROUP BY FUNCTION('DATE_FORMAT', s.orderDate, '%Y-%m') " +
           "ORDER BY FUNCTION('DATE_FORMAT', s.orderDate, '%Y-%m') ASC")
    List<Object[]> findMonthlyRevenueTrends();

    @Query("SELECT p.name, SUM(si.quantity), SUM(si.subtotal) " +
           "FROM SaleItem si " +
           "JOIN si.product p " +
           "JOIN si.sale s " +
           "WHERE s.status = 'COMPLETED' " +
           "GROUP BY p.id, p.name " +
           "ORDER BY SUM(si.quantity) DESC")
    List<Object[]> findTopSellingProducts();

    @Query("SELECT COALESCE(c.country, 'Unknown'), COUNT(s), COALESCE(SUM(s.totalAmount), 0) " +
           "FROM Sale s JOIN s.customer c " +
           "WHERE s.status = 'COMPLETED' " +
           "GROUP BY COALESCE(c.country, 'Unknown') " +
           "ORDER BY SUM(s.totalAmount) DESC")
    List<Object[]> findRevenueByCountry();
}
