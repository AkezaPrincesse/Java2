package com.exam.utility.service;

import java.math.BigDecimal;
import java.util.Map;

public interface EmailService {
    void sendVerificationEmail(String to, String name, String token);
    void sendOtpEmail(String to, String name, String otp, String purpose);
    void sendPasswordResetEmail(String to, String name, String token, String otp);
    void sendWelcomeEmail(String to, String name);
    void sendBillGeneratedEmail(String to, String name, String billNumber, BigDecimal amount, String dueDate);
    void sendBillApprovedEmail(String to, String name, String billNumber, BigDecimal amount);
    void sendPaymentConfirmationEmail(String to, String name, String receiptNumber, BigDecimal amount, String billNumber, BigDecimal remainingBalance);
    void sendOverdueBillEmail(String to, String name, String billNumber, BigDecimal amount);
    /** Sends login credentials to a newly created staff account. */
    void sendUserCreatedEmail(String to, String fullName, String temporaryPassword, String role, String loginUrl);

    /** Notifies a user that their role has been changed. */
    void sendRoleChangedEmail(String to, String fullName, String previousRole, String newRole, String permissionDescription);

    void sendEmail(String to, String subject, String templateName, Map<String, Object> variables);
}
