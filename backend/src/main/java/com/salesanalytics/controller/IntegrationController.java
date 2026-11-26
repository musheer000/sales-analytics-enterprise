package com.salesanalytics.controller;

import com.salesanalytics.dto.ApiResponse;
import com.salesanalytics.dto.SaleDTO;
import com.salesanalytics.dto.SaleItemDTO;
import com.salesanalytics.service.EmailService;
import com.salesanalytics.service.RealtimeEventService;
import com.salesanalytics.service.SaleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/integrations")
@CrossOrigin(origins = "*")
public class IntegrationController {

    private final EmailService emailService;
    private final SaleService saleService;
    private final RealtimeEventService realtimeEventService;

    @Value("${app.razorpay.key-id:rzp_test_TWMghMtesxGj8a}")
    private String razorpayKeyId;

    @Value("${app.mail.username:shaikmusheer07@gmail.com}")
    private String senderEmail;

    public IntegrationController(EmailService emailService, SaleService saleService, RealtimeEventService realtimeEventService) {
        this.emailService = emailService;
        this.saleService = saleService;
        this.realtimeEventService = realtimeEventService;
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPublicConfig() {
        return ResponseEntity.ok(ApiResponse.ok("Integration config", Map.of(
                "razorpayKeyId", razorpayKeyId,
                "emailConfigured", true,
                "senderEmail", senderEmail
        )));
    }

    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendEmail(@RequestBody Map<String, String> body) {
        String to = body.getOrDefault("to", senderEmail);
        String subject = body.getOrDefault("subject", "Sales Analytics Report");
        String htmlBody = body.getOrDefault("htmlBody", "<p>Sales Analytics notification.</p>");
        emailService.sendHtmlEmail(to, subject, htmlBody);
        return ResponseEntity.ok(ApiResponse.ok("Email sent successfully to " + to, Map.of("to", to, "status", "SENT")));
    }

    @PostMapping("/email/invoice/{saleId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> emailInvoice(
            @PathVariable Long saleId,
            @RequestBody(required = false) Map<String, String> body) {
        SaleDTO sale = saleService.getSaleById(saleId);
        String to = (body != null && body.get("to") != null && !body.get("to").isBlank())
                ? body.get("to").trim()
                : (sale.getCustomerEmail() != null && !sale.getCustomerEmail().isBlank() ? sale.getCustomerEmail() : senderEmail);

        StringBuilder rows = new StringBuilder();
        if (sale.getItems() != null) {
            for (SaleItemDTO item : sale.getItems()) {
                rows.append("<tr>")
                    .append("<td style='padding:10px;border-bottom:1px solid #e5e7eb;'>").append(escape(item.getProductName())).append("</td>")
                    .append("<td style='padding:10px;border-bottom:1px solid #e5e7eb;text-align:center;'>").append(item.getQuantity()).append("</td>")
                    .append("<td style='padding:10px;border-bottom:1px solid #e5e7eb;text-align:right;'>$").append(item.getUnitPrice()).append("</td>")
                    .append("<td style='padding:10px;border-bottom:1px solid #e5e7eb;text-align:right;font-weight:600;'>$").append(item.getSubtotal()).append("</td>")
                    .append("</tr>");
            }
        }

        String html = "<div style='font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;max-width:620px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;'>"
                + "<div style='background:linear-gradient(135deg,#007AFF,#5856D6);color:#fff;padding:24px 28px;'>"
                + "<h2 style='margin:0;font-size:20px;'>Sales Analytics Enterprise — Invoice</h2>"
                + "<p style='margin:6px 0 0;opacity:0.9;font-size:13px;'>Order " + escape(sale.getOrderNumber()) + " • " + escape(sale.getStatus()) + "</p>"
                + "</div>"
                + "<div style='padding:24px 28px;color:#111827;'>"
                + "<p style='margin:0 0 14px;'>Hello <strong>" + escape(sale.getCustomerName()) + "</strong>,</p>"
                + "<p style='margin:0 0 18px;color:#4b5563;font-size:14px;'>Thank you for your order. Payment method: <strong>" + escape(sale.getPaymentMethod()) + "</strong>"
                + (sale.getNotes() != null && !sale.getNotes().isBlank() ? " (" + escape(sale.getNotes()) + ")" : "") + ".</p>"
                + "<table style='width:100%;border-collapse:collapse;font-size:14px;margin-bottom:18px;'>"
                + "<thead><tr style='background:#f9fafb;text-align:left;'>"
                + "<th style='padding:10px;border-bottom:1px solid #e5e7eb;'>Product</th>"
                + "<th style='padding:10px;border-bottom:1px solid #e5e7eb;text-align:center;'>Qty</th>"
                + "<th style='padding:10px;border-bottom:1px solid #e5e7eb;text-align:right;'>Unit Price</th>"
                + "<th style='padding:10px;border-bottom:1px solid #e5e7eb;text-align:right;'>Subtotal</th>"
                + "</tr></thead><tbody>" + rows + "</tbody></table>"
                + "<div style='text-align:right;font-size:18px;font-weight:700;color:#007AFF;'>Total: $" + sale.getTotalAmount() + " USD</div>"
                + "</div>"
                + "<div style='background:#f9fafb;padding:14px 28px;font-size:12px;color:#6b7280;text-align:center;'>"
                + "Sent via Sales Analytics Enterprise (" + escape(senderEmail) + ")"
                + "</div></div>";

        emailService.sendHtmlEmail(to, "Invoice " + sale.getOrderNumber() + " — Sales Analytics Enterprise", html);
        realtimeEventService.publish("INVOICE_EMAILED", "Invoice " + sale.getOrderNumber() + " sent to " + to, "admin");
        return ResponseEntity.ok(ApiResponse.ok("Invoice emailed to " + to, Map.of("to", to, "orderNumber", sale.getOrderNumber())));
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
