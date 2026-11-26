package com.salesanalytics.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final long OTP_TTL_SECONDS = 300; // 5 minutes
    private static final long RESEND_COOLDOWN_SECONDS = 30;
    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom secureRandom = new SecureRandom();
    private final EmailService emailService;

    private static class OtpEntry {
        final String code;
        final Instant expiresAt;
        final Instant createdAt;
        int attempts;

        OtpEntry(String code) {
            this.code = code;
            this.createdAt = Instant.now();
            this.expiresAt = this.createdAt.plusSeconds(OTP_TTL_SECONDS);
            this.attempts = 0;
        }
    }

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    public OtpService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void sendOtp(String email, String purpose, String displayName) {
        String key = buildKey(email, purpose);
        OtpEntry existing = otpStore.get(key);
        if (existing != null && Instant.now().isBefore(existing.createdAt.plusSeconds(RESEND_COOLDOWN_SECONDS))) {
            long waitSec = existing.createdAt.plusSeconds(RESEND_COOLDOWN_SECONDS).getEpochSecond() - Instant.now().getEpochSecond();
            throw new IllegalStateException("Please wait " + Math.max(1, waitSec) + "s before requesting another OTP.");
        }

        String otp = String.format("%06d", 100000 + secureRandom.nextInt(900000));
        otpStore.put(key, new OtpEntry(otp));

        String purposeTitle = "REGISTER".equalsIgnoreCase(purpose)
                ? "Account Registration Verification"
                : "Two-Factor Login Verification";
        String safeName = (displayName == null || displayName.isBlank()) ? "User" : escape(displayName);

        String html = "<div style='font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;max-width:520px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:16px;overflow:hidden;'>"
                + "<div style='background:linear-gradient(135deg,#007AFF,#5856D6);color:#ffffff;padding:24px 28px;'>"
                + "<h2 style='margin:0;font-size:20px;'>Sales Analytics Enterprise</h2>"
                + "<p style='margin:6px 0 0;opacity:0.9;font-size:13px;'>" + purposeTitle + "</p>"
                + "</div>"
                + "<div style='padding:28px;color:#111827;'>"
                + "<p style='margin:0 0 14px;font-size:15px;'>Hello <strong>" + safeName + "</strong>,</p>"
                + "<p style='margin:0 0 20px;color:#4b5563;font-size:14px;line-height:1.5;'>Use the 6-digit One-Time Password (OTP) below to complete your verification. This code expires in <strong>5 minutes</strong>.</p>"
                + "<div style='background:#f3f4f6;border:1px dashed #007AFF;border-radius:12px;padding:18px;text-align:center;margin:0 0 20px;'>"
                + "<span style='font-family:monospace;font-size:32px;font-weight:800;letter-spacing:8px;color:#007AFF;'>" + otp + "</span>"
                + "</div>"
                + "<p style='margin:0;color:#6b7280;font-size:12px;'>If you did not request this code, you can safely ignore this email. Never share your OTP with anyone.</p>"
                + "</div>"
                + "<div style='background:#f9fafb;padding:14px 28px;font-size:11px;color:#9ca3af;text-align:center;'>"
                + "Secured by Sales Analytics 2FA &bull; JWT + BCrypt + Gmail SMTP"
                + "</div></div>";

        emailService.sendHtmlEmail(email, otp + " is your Sales Analytics verification code", html);
    }

    public void verifyOtp(String email, String purpose, String code) {
        if (code == null || code.trim().length() != 6) {
            throw new IllegalArgumentException("Please enter a valid 6-digit OTP code.");
        }
        String key = buildKey(email, purpose);
        OtpEntry entry = otpStore.get(key);
        if (entry == null) {
            throw new IllegalArgumentException("No active OTP found. Please request a new code.");
        }
        if (Instant.now().isAfter(entry.expiresAt)) {
            otpStore.remove(key);
            throw new IllegalArgumentException("OTP has expired. Please request a new code.");
        }
        entry.attempts++;
        if (entry.attempts > MAX_ATTEMPTS) {
            otpStore.remove(key);
            throw new IllegalArgumentException("Too many failed attempts. Please request a new OTP.");
        }
        if (!entry.code.equals(code.trim())) {
            int remaining = MAX_ATTEMPTS - entry.attempts;
            throw new IllegalArgumentException("Invalid OTP code. " + remaining + " attempt(s) remaining.");
        }
        otpStore.remove(key);
    }

    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String prefix = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return prefix + "***@" + parts[1];
    }

    private String buildKey(String email, String purpose) {
        return purpose.toUpperCase() + ":" + email.trim().toLowerCase();
    }

    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
