package com.salesanalytics.repository;

import com.salesanalytics.entity.UserOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserOrderRepository extends JpaRepository<UserOrder, Long> {

    // Eagerly fetch product to avoid LazyInitializationException on user portal
    @Query("SELECT o FROM UserOrder o JOIN FETCH o.product WHERE o.userEmail = :email ORDER BY o.createdAt DESC")
    List<UserOrder> findByUserEmailWithProduct(@Param("email") String email);

    @Query("SELECT o FROM UserOrder o JOIN FETCH o.product ORDER BY o.createdAt DESC")
    List<UserOrder> findAllWithProduct();

    @Query("SELECT COALESCE(o.buyerCountry, 'India'), COALESCE(o.buyerRegion, 'South Asia'), COUNT(o), COALESCE(SUM(o.quantity), 0), COALESCE(SUM(o.totalAmount), 0) " +
           "FROM UserOrder o GROUP BY COALESCE(o.buyerCountry, 'India'), COALESCE(o.buyerRegion, 'South Asia') ORDER BY SUM(o.totalAmount) DESC")
    List<Object[]> findUserOrderStatsByCountry();

    @Query("SELECT COALESCE(o.orderType, 'RETAIL'), COUNT(o), COALESCE(SUM(o.quantity), 0), COALESCE(SUM(o.totalAmount), 0) " +
           "FROM UserOrder o GROUP BY COALESCE(o.orderType, 'RETAIL')")
    List<Object[]> findUserOrderStatsByType();

    List<UserOrder> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    Optional<UserOrder> findByOrderRef(String orderRef);

    List<UserOrder> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);
}
