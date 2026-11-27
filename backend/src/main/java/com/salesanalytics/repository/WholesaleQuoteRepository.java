package com.salesanalytics.repository;

import com.salesanalytics.entity.WholesaleQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface WholesaleQuoteRepository extends JpaRepository<WholesaleQuote, Long> {

    @Query("SELECT q FROM WholesaleQuote q JOIN FETCH q.product WHERE q.buyerEmail = :email ORDER BY q.createdAt DESC")
    List<WholesaleQuote> findByBuyerEmailWithProduct(@Param("email") String email);

    @Query("SELECT q FROM WholesaleQuote q JOIN FETCH q.product WHERE q.sellerEmail = :email ORDER BY q.createdAt DESC")
    List<WholesaleQuote> findBySellerEmailWithProduct(@Param("email") String email);

    @Query("SELECT q FROM WholesaleQuote q JOIN FETCH q.product ORDER BY q.createdAt DESC")
    List<WholesaleQuote> findAllWithProduct();

    Optional<WholesaleQuote> findByQuoteRef(String quoteRef);

    long countByStatus(String status);
}
