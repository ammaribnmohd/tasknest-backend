package com.example.taskflow_backend.service.impl;

import com.example.taskflow_backend.config.AppProperties;
import com.example.taskflow_backend.exception.EmailDeliveryException;
import com.example.taskflow_backend.service.EmailService;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class BrevoEmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailServiceImpl.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;
    private final RestClient restClient = RestClient.create();

    public BrevoEmailServiceImpl(JavaMailSender mailSender, AppProperties appProperties) {
        this.mailSender = mailSender;
        this.appProperties = appProperties;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String fullName, String otpCode, int expiryMinutes) {
        String subject = "Verify your Taskflow account";
        String htmlContent = buildEmailTemplate(
                fullName,
                "Welcome to Taskflow! To finish setting up your account, please enter the 6-digit verification code below:",
                otpCode,
                expiryMinutes,
                "If you did not request this verification, you can safely ignore this email."
        );
        String textContent = "Hello " + (fullName != null ? fullName : "there") + ",\n\n"
                + "Your Taskflow verification code is: " + otpCode + "\n\n"
                + "This code expires in " + expiryMinutes + " minutes.\n"
                + "If you did not sign up for Taskflow, you can safely ignore this email.";

        sendEmail(toEmail, subject, htmlContent, textContent);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String fullName, String otpCode, int expiryMinutes) {
        String subject = "Taskflow Password Reset Code";
        String htmlContent = buildEmailTemplate(
                fullName,
                "We received a request to reset your Taskflow password. Use the 6-digit code below to set a new password:",
                otpCode,
                expiryMinutes,
                "If you did not ask to reset your password, please ignore this email or review your account security."
        );
        String textContent = "Hello " + (fullName != null ? fullName : "there") + ",\n\n"
                + "Your Taskflow password reset code is: " + otpCode + "\n\n"
                + "This code expires in " + expiryMinutes + " minutes.\n"
                + "If you did not make this request, you can safely ignore this email.";

        sendEmail(toEmail, subject, htmlContent, textContent);
    }

    private void sendEmail(String toEmail, String subject, String htmlContent, String textContent) {
        String fromEmail = appProperties.mail().from();
        String fromName = appProperties.mail().fromName();
        if (fromEmail == null || fromEmail.isBlank()) {
            fromEmail = "ammaribn2@gmail.com";
        }
        if (fromName == null || fromName.isBlank()) {
            fromName = "Taskflow";
        }

        String brevoApiKey = appProperties.mail().brevoApiKey();
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            sendViaBrevoApi(brevoApiKey.trim(), fromEmail, fromName, toEmail, subject, htmlContent, textContent);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(textContent, htmlContent);

            mailSender.send(message);
            log.info("Sent email via SMTP successfully to: {}", toEmail);
        } catch (Exception ex) {
            log.error("Failed to send email via SMTP to {}: {}", toEmail, ex.getMessage(), ex);
            throw new EmailDeliveryException("Failed to deliver email: " + ex.getMessage(), ex);
        }
    }

    private void sendViaBrevoApi(
            String apiKey,
            String fromEmail,
            String fromName,
            String toEmail,
            String subject,
            String htmlContent,
            String textContent
    ) {
        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", fromName, "email", fromEmail),
                    "to", List.of(Map.of("email", toEmail)),
                    "subject", subject,
                    "htmlContent", htmlContent,
                    "textContent", textContent
            );

            restClient.post()
                    .uri(BREVO_API_URL)
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Sent email via Brevo HTTPS API successfully to: {}", toEmail);
        } catch (Exception ex) {
            log.error("Failed to send email via Brevo HTTPS API to {}: {}", toEmail, ex.getMessage(), ex);
            throw new EmailDeliveryException("Failed to deliver email: " + ex.getMessage(), ex);
        }
    }

    private String buildEmailTemplate(String name, String introMessage, String otpCode, int expiryMinutes, String disclaimer) {
        String greeting = (name != null && !name.isBlank()) ? "Hello " + name + "," : "Hello,";
        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Taskflow Security Code</title>
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
            .container { max-width: 480px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0; padding: 32px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05); }
            .logo { font-size: 20px; font-weight: 700; color: #0284c7; letter-spacing: -0.5px; margin-bottom: 24px; display: inline-block; }
            .title { font-size: 18px; font-weight: 600; color: #0f172a; margin-top: 0; margin-bottom: 12px; }
            .text { font-size: 14px; line-height: 1.6; color: #475569; margin-bottom: 24px; }
            .code-box { background-color: #f1f5f9; border: 1px dashed #cbd5e1; border-radius: 8px; padding: 18px; text-align: center; margin-bottom: 24px; }
            .code { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace; font-size: 32px; font-weight: 700; letter-spacing: 6px; color: #0f172a; }
            .meta { font-size: 13px; color: #64748b; margin-top: 8px; }
            .footer { font-size: 12px; color: #94a3b8; border-top: 1px solid #f1f5f9; padding-top: 16px; margin-top: 24px; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="logo">Taskflow</div>
            <h1 class="title">%s</h1>
            <p class="text">%s</p>
            <div class="code-box">
              <div class="code">%s</div>
              <div class="meta">Valid for %d minutes. Never share this code.</div>
            </div>
            <div class="footer">%s</div>
          </div>
        </body>
        </html>
        """.formatted(greeting, introMessage, otpCode, expiryMinutes, disclaimer);
    }
}

