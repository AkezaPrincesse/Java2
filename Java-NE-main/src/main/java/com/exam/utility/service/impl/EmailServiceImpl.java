package com.exam.utility.service.impl;

import com.exam.utility.entity.EmailLog;
import com.exam.utility.repository.EmailLogRepository;
import com.exam.utility.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Sends transactional emails using Thymeleaf HTML templates and JavaMailSender.
 *
 * All sends are @Async (emailExecutor thread pool) so they never block the request thread.
 * Every send attempt — success or failure — is logged to the email_logs table for auditing.
 *
 * Supported events:
 * - Email verification (24h token)
 * - Welcome (sent AFTER verification, not at registration)
 * - OTP codes (10 min expiry)
 * - Password reset (30 min token + OTP)
 * - New user account credentials (admin-created accounts with temporary password)
 * - Role change notification
 * - Bill generated / approved / overdue
 * - Payment confirmation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailLogRepository emailLogRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    @Async("emailExecutor")
    public void sendVerificationEmail(String to, String name, String token) {
        Map<String, Object> vars = Map.of(
            "name", name,
            "verificationLink", frontendUrl + "/verify-email?token=" + token,
            "expiryHours", 24
        );
        sendEmail(to, "Verify Your Email – WASAC/REG Billing", "email/verification", vars);
    }

    @Override
    @Async("emailExecutor")
    public void sendOtpEmail(String to, String name, String otp, String purpose) {
        Map<String, Object> vars = Map.of(
            "name", name,
            "otp", otp,
            "purpose", purpose,
            "expiryMinutes", 10
        );
        sendEmail(to, "Your OTP Code – WASAC/REG Billing", "email/otp", vars);
    }

    @Override
    @Async("emailExecutor")
    public void sendPasswordResetEmail(String to, String name, String token, String otp) {
        Map<String, Object> vars = Map.of(
            "name", name,
            "resetLink", frontendUrl + "/reset-password?token=" + token,
            "otp", otp,
            "expiryMinutes", 30
        );
        sendEmail(to, "Password Reset – WASAC/REG Billing", "email/password-reset", vars);
    }

    @Override
    @Async("emailExecutor")
    public void sendWelcomeEmail(String to, String name) {
        Map<String, Object> vars = Map.of("name", name);
        sendEmail(to, "Welcome to WASAC/REG Utility Billing", "email/welcome", vars);
    }

    @Override
    @Async("emailExecutor")
    public void sendBillGeneratedEmail(String to, String name, String billNumber,
                                        BigDecimal amount, String dueDate) {
        Map<String, Object> vars = Map.of(
            "name", name,
            "billNumber", billNumber,
            "amount", amount,
            "dueDate", dueDate
        );
        sendEmail(to, "New Bill Generated – " + billNumber, "email/bill-generated", vars);
    }

    @Override
    @Async("emailExecutor")
    public void sendBillApprovedEmail(String to, String name, String billNumber, BigDecimal amount) {
        Map<String, Object> vars = Map.of("name", name, "billNumber", billNumber, "amount", amount);
        sendEmail(to, "Bill Approved – " + billNumber, "email/bill-approved", vars);
    }

    @Override
    @Async("emailExecutor")
    public void sendPaymentConfirmationEmail(String to, String name, String receiptNumber,
                                              BigDecimal amount, String billNumber, BigDecimal remainingBalance) {
        Map<String, Object> vars = new java.util.HashMap<>();
        vars.put("name", name);
        vars.put("receiptNumber", receiptNumber);
        vars.put("amount", amount);
        vars.put("billNumber", billNumber);
        vars.put("paymentDate", LocalDateTime.now().toString());
        vars.put("remainingBalance", remainingBalance);
        vars.put("isFullyPaid", remainingBalance.compareTo(BigDecimal.ZERO) <= 0);
        sendEmail(to, "Payment Confirmed – Receipt " + receiptNumber, "email/payment-confirmation", vars);
    }

    @Override
    @Async("emailExecutor")
    public void sendOverdueBillEmail(String to, String name, String billNumber, BigDecimal amount) {
        Map<String, Object> vars = Map.of("name", name, "billNumber", billNumber, "amount", amount);
        sendEmail(to, "Overdue Bill Notice – " + billNumber, "email/overdue-bill", vars);
    }

    @Override
    @Async("emailExecutor")
    public void sendUserCreatedEmail(String to, String fullName, String temporaryPassword, String role, String loginUrl) {
        Map<String, Object> vars = Map.of(
            "name", fullName,
            "email", to,
            "temporaryPassword", temporaryPassword,
            "role", role,
            "loginUrl", loginUrl
        );
        sendEmail(to, "Utility Billing System Account Created", "email/user-created", vars);
    }

    @Override
    @Async("emailExecutor")
    public void sendRoleChangedEmail(String to, String fullName, String previousRole, String newRole, String permissionDescription) {
        Map<String, Object> vars = Map.of(
            "name", fullName,
            "previousRole", previousRole,
            "newRole", newRole,
            "permissionDescription", permissionDescription,
            "changeDate", java.time.LocalDate.now().toString()
        );
        sendEmail(to, "Your Role Has Been Updated – Utility Billing System", "email/role-changed", vars);
    }

    @Override
    public void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        boolean sent = false;
        String errorMsg = null;
        try {
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            sent = true;
            log.info("Email sent to {} subject: {}", to, subject);
        } catch (Exception e) {
            errorMsg = e.getMessage();
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        } finally {
            emailLogRepository.save(EmailLog.builder()
                .toEmail(to)
                .subject(subject)
                .templateName(templateName)
                .sent(sent)
                .errorMessage(errorMsg)
                .sentAt(LocalDateTime.now())
                .build());
        }
    }
}
