package com.salesanalytics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${app.mail.port:465}")
    private int mailPort;

    @Value("${app.mail.username:shaikmusheer07@gmail.com}")
    private String username;

    @Value("${app.mail.password:krkuuvbflgpntwnt}")
    private String password;

    @Value("${app.mail.from-name:Sales Analytics Enterprise}")
    private String fromName;

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient email address is required");
        }
        String cleanPassword = password == null ? "" : password.replaceAll("\\s+", "");
        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(mailHost, mailPort)) {
                socket.setSoTimeout(15000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

                expect(reader, "220");
                sendCmd(writer, reader, "EHLO localhost", "250");
                sendCmd(writer, reader, "AUTH LOGIN", "334");
                sendCmd(writer, reader, Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)), "334");
                sendCmd(writer, reader, Base64.getEncoder().encodeToString(cleanPassword.getBytes(StandardCharsets.UTF_8)), "235");
                sendCmd(writer, reader, "MAIL FROM:<" + username + ">", "250");
                sendCmd(writer, reader, "RCPT TO:<" + to.trim() + ">", "250");
                sendCmd(writer, reader, "DATA", "354");

                StringBuilder msg = new StringBuilder();
                msg.append("From: ").append(fromName).append(" <").append(username).append(">\r\n");
                msg.append("To: <").append(to.trim()).append(">\r\n");
                msg.append("Subject: ").append(subject).append("\r\n");
                msg.append("MIME-Version: 1.0\r\n");
                msg.append("Content-Type: text/html; charset=UTF-8\r\n");
                msg.append("\r\n");
                msg.append(htmlBody.replace("\n.", "\n.."));
                msg.append("\r\n.\r\n");

                writer.write(msg.toString());
                writer.flush();
                expect(reader, "250");
                sendCmd(writer, reader, "QUIT", "221");
                log.info("Email sent via Gmail SMTP to {}", to);
            }
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email delivery failed: " + e.getMessage(), e);
        }
    }

    private void sendCmd(BufferedWriter writer, BufferedReader reader, String cmd, String expectedPrefix) throws IOException {
        writer.write(cmd + "\r\n");
        writer.flush();
        expect(reader, expectedPrefix);
    }

    private void expect(BufferedReader reader, String expectedPrefix) throws IOException {
        String line;
        String lastLine = null;
        while ((line = reader.readLine()) != null) {
            lastLine = line;
            if (line.length() < 4 || line.charAt(3) == ' ') {
                break;
            }
        }
        if (lastLine == null || !lastLine.startsWith(expectedPrefix)) {
            throw new IOException("SMTP error (expected " + expectedPrefix + "): " + lastLine);
        }
    }
}
