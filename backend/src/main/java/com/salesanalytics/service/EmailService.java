package com.salesanalytics.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Email delivery service using Spring Boot JavaMailSender (Jakarta Mail).
 * Uses STARTTLS on port 587 — works on Render free tier (port 465 is blocked).
 * sendHtmlEmail is @Async so it never blocks the HTTP request thread.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.mail.from-name:Sales Analytics Enterprise}")
    private String fromName;

    /**
     * Sends an HTML email asynchronously.
     * The OTP is already stored in OtpService before this method is called,
     * so the caller does not need to wait for delivery.
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient email address is required");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            // Don't rethrow — async method; caller already received HTTP response
        }
    }
}
