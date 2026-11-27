package com.salesanalytics.repository;

import com.salesanalytics.entity.UserOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserOrderRepository extends JpaRepository<UserOrder, Long> {

    List<UserOrder> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    Optional<UserOrder> findByOrderRef(String orderRef);

    List<UserOrder> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);
}
