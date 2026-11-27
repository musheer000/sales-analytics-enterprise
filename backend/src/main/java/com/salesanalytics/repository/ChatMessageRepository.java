package com.salesanalytics.repository;

import com.salesanalytics.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Get full thread between user and admin (both directions)
    @Query("SELECT c FROM ChatMessage c WHERE " +
           "(c.senderEmail = :userEmail OR c.recipientEmail = :userEmail) " +
           "ORDER BY c.createdAt ASC")
    List<ChatMessage> findThreadByUserEmail(@Param("userEmail") String userEmail);

    // All messages for admin (all users)
    List<ChatMessage> findAllByOrderByCreatedAtDesc();

    // Unread count for admin
    @Query("SELECT COUNT(c) FROM ChatMessage c WHERE c.senderRole = 'USER' AND c.read = false")
    long countUnreadForAdmin();

    // Unread count for a specific user (admin replies to them)
    @Query("SELECT COUNT(c) FROM ChatMessage c WHERE c.recipientEmail = :email AND c.read = false")
    long countUnreadForUser(@Param("email") String email);

    // Distinct users who have ever sent a message (for admin inbox)
    @Query("SELECT DISTINCT c.senderEmail FROM ChatMessage c WHERE c.senderRole = 'USER'")
    List<String> findDistinctUserSenders();
}
