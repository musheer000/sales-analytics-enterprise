package com.salesanalytics.repository;

import com.salesanalytics.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT c.id, c.firstName, c.lastName, c.email, c.phone, c.city, c.country, " +
           "COUNT(s.id), COALESCE(SUM(s.totalAmount), 0), MAX(s.orderDate) " +
           "FROM Customer c LEFT JOIN Sale s ON s.customer.id = c.id " +
           "GROUP BY c.id, c.firstName, c.lastName, c.email, c.phone, c.city, c.country " +
           "ORDER BY COALESCE(SUM(s.totalAmount), 0) DESC")
    List<Object[]> findCustomersWithStats();
}
