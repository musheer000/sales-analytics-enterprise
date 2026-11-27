package com.salesanalytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Email delivery service supporting:
 * 1. Brevo HTTP API (BREVO_API_KEY) — works on Render free tier (port 443 HTTPS)
 * 2. Google Apps Script / Custom Webhook (MAIL_WEBHOOK_URL) — works on Render free tier (port 443 HTTPS)
 * 3. Spring JavaMailSender SMTP fallback — for local development (Render blocks outbound SMTP)
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.mail.username:shaikmusheer07@gmail.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:Sales Analytics Enterprise}")
    private String fromName;

    @Value("${app.mail.brevo-api-key:}")
    private String brevoApiKey;

    @Value("${app.mail.webhook-url:}")
    private String webhookUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient email address is required");
        }
        String recipient = to.trim();

        // 1. Prefer Brevo HTTP API (port 443 — never blocked on Render)
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            sendViaBrevo(recipient, subject, htmlBody);
            return;
        }

        // 2. Next prefer Google Apps Script / Webhook (port 443 — never blocked on Render)
        if (webhookUrl != null && !webhookUrl.isBlank()) {
            sendViaWebhook(recipient, subject, htmlBody);
            return;
        }

        // 3. Fallback to SMTP (works locally; Render free tier blocks outbound SMTP 25/465/587)
        sendViaSmtp(recipient, subject, htmlBody);
    }

    private void sendViaBrevo(String to, String subject, String htmlBody) {
        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", fromName, "email", fromEmail),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", htmlBody
            );
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(12))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("api-key", brevoApiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Brevo API error ({}): {}", response.statusCode(), response.body());
                throw new RuntimeException("Email delivery failed (Brevo HTTP " + response.statusCode() + ")");
            }
            log.info("Email sent via Brevo API to {}", to);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Brevo email failed to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email delivery failed: " + e.getMessage(), e);
        }
    }

    private void sendViaWebhook(String to, String subject, String htmlBody) {
        try {
            Map<String, Object> payload = Map.of(
                    "to", to,
                    "subject", subject,
                    "htmlBody", htmlBody,
                    "fromName", fromName
            );
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl.trim()))
                    .timeout(Duration.ofSeconds(15))
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 400) {
                log.error("Mail webhook error ({}): {}", response.statusCode(), response.body());
                throw new RuntimeException("Email delivery failed (Webhook HTTP " + response.statusCode() + ")");
            }
            log.info("Email sent via Mail Webhook to {}", to);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Webhook email failed to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email delivery failed: " + e.getMessage(), e);
        }
    }

    private void sendViaSmtp(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent via SMTP to {}", to);
        } catch (Exception e) {
            log.error("SMTP email failed to {}: {}", to, e.getMessage());
            throw new RuntimeException(
                    "SMTP email failed (Render blocks SMTP ports 25/465/587). Configure BREVO_API_KEY or MAIL_WEBHOOK_URL on Render.",
                    e);
        }
    }
}
