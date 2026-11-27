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

    List<UserOrder> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    Optional<UserOrder> findByOrderRef(String orderRef);

    List<UserOrder> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);
}
