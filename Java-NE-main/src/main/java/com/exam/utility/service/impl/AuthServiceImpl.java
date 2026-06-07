package com.exam.utility.service.impl;

import com.exam.utility.dto.request.auth.*;
import com.exam.utility.dto.request.auth.ChangePasswordRequest;
import com.exam.utility.dto.response.auth.AuthResponse;
import com.exam.utility.dto.response.auth.TokenRefreshResponse;
import com.exam.utility.dto.response.auth.UserResponse;
import com.exam.utility.entity.*;
import com.exam.utility.enums.AuditAction;
import com.exam.utility.exception.*;
import com.exam.utility.entity.Customer;
import com.exam.utility.repository.*;
import com.exam.utility.security.JwtService;
import com.exam.utility.service.AuditService;
import com.exam.utility.service.AuthService;
import com.exam.utility.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles the full authentication lifecycle.
 *
 * Key business rules:
 * - Self-registered users are disabled until they verify their email.
 * - Welcome email is sent ONLY after successful email verification, not at registration.
 * - Failed login attempts are tracked; accounts lock after 5 failures for 30 minutes.
 * - Password reset requires both a token (30 min expiry) and a 6-digit OTP (10 min expiry).
 * - Admin-created users have forcePasswordChange = true; they must call /auth/change-password
 *   before accessing any other protected endpoint.
 * - All refresh tokens are revoked on logout and on password change.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final AuditService auditService;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Role defaultRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new ResourceNotFoundException("Default role not configured"));

        User user = User.builder()
            .fullName(request.getFullName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .phoneNumber(request.getPhoneNumber())
            .enabled(false)
            .roles(Set.of(defaultRole))
            .build();

        userRepository.save(user);

        String verificationToken = UUID.randomUUID().toString();
        verificationTokenRepository.save(VerificationToken.builder()
            .token(verificationToken)
            .user(user)
            .expiryDate(LocalDateTime.now().plusHours(24))
            .build());

        // Welcome email is deferred until the user successfully verifies their email address
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationToken);

        auditService.log(AuditAction.CREATE, "User", user.getId().toString(),
            "New user registered: " + user.getEmail());

        log.info("New user registered: {}", user.getEmail());
        return buildAuthResponse(user, null, null);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (!user.isAccountNonLocked()) {
            if (user.getLockTime() != null &&
                user.getLockTime().plusMinutes(LOCK_DURATION_MINUTES).isAfter(LocalDateTime.now())) {
                auditService.logLogin(request.getEmail(), false, httpRequest);
                throw new BusinessException("Account is locked. Try again in " + LOCK_DURATION_MINUTES + " minutes.");
            } else {
                userRepository.resetFailedAttempts(request.getEmail());
            }
        }

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            userRepository.incrementFailedAttempts(request.getEmail());
            int attempts = user.getFailedLoginAttempts() + 1;
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setAccountNonLocked(false);
                user.setLockTime(LocalDateTime.now());
                userRepository.save(user);
            }
            auditService.logLogin(request.getEmail(), false, httpRequest);
            throw e;
        }

        userRepository.resetFailedAttempts(request.getEmail());
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId());
        String accessToken = jwtService.generateToken(user);
        String refreshTokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = refreshTokenRepository.save(RefreshToken.builder()
            .token(refreshTokenValue)
            .user(user)
            .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
            .ipAddress(httpRequest.getRemoteAddr())
            .userAgent(httpRequest.getHeader("User-Agent"))
            .build());

        auditService.logLogin(request.getEmail(), true, httpRequest);
        log.info("User logged in: {}", user.getEmail());

        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    @Override
    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
            .orElseThrow(() -> new TokenException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new TokenException("Refresh token is expired or revoked. Please login again.");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateToken(user);

        return TokenRefreshResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken.getToken())
            .build();
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
            userRepository.findByEmail(currentUser).ifPresent(user -> {
                refreshTokenRepository.revokeAllByUserId(user.getId());
                auditService.log(AuditAction.LOGOUT, "User", user.getId().toString(), "User logged out");
            });
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
            .orElseThrow(() -> new TokenException("Invalid verification token"));

        if (verificationToken.isUsed()) {
            throw new TokenException("Verification token already used");
        }
        if (verificationToken.isExpired()) {
            throw new TokenException("Verification token has expired. Please request a new one.");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

        // Link to an existing Customer record with the same email if one exists
        customerRepository.findByEmail(user.getEmail()).ifPresent(customer -> {
            if (customer.getUser() == null) {
                customer.setUser(user);
                customerRepository.save(customer);
                log.info("Linked verified user {} to existing customer record {}", user.getEmail(), customer.getId());
            }
        });

        // Send welcome email only after successful verification, per security policy
        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());

        auditService.log(AuditAction.EMAIL_VERIFIED, "User", user.getId().toString(),
            "Email verified: " + user.getEmail());
        log.info("Email verified for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + request.getEmail()));

        passwordResetTokenRepository.invalidateAllByUserId(user.getId());
        String resetToken = UUID.randomUUID().toString();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
            .token(resetToken)
            .user(user)
            .expiryDate(LocalDateTime.now().plusMinutes(30))
            .build());

        String otp = generateOtp();
        otpCodeRepository.invalidateAll(user.getEmail(), "PASSWORD_RESET");
        otpCodeRepository.save(OtpCode.builder()
            .code(otp)
            .email(user.getEmail())
            .purpose("PASSWORD_RESET")
            .generatedAt(LocalDateTime.now())
            .expiryAt(LocalDateTime.now().plusMinutes(10))
            .build());

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetToken, otp);
        log.info("Password reset initiated for: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
            .orElseThrow(() -> new TokenException("Invalid password reset token"));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new TokenException("Password reset token is invalid or expired");
        }

        OtpCode otp = otpCodeRepository
            .findTopByEmailAndPurposeAndUsedFalseOrderByGeneratedAtDesc(
                resetToken.getUser().getEmail(), "PASSWORD_RESET")
            .orElseThrow(() -> new TokenException("OTP not found or already used"));

        if (!otp.getCode().equals(request.getOtp()) || otp.isExpired()) {
            throw new TokenException("Invalid or expired OTP");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        otp.setUsed(true);
        otpCodeRepository.save(otp);

        refreshTokenRepository.revokeAllByUserId(user.getId());
        auditService.log(AuditAction.PASSWORD_CHANGE, "User", user.getId().toString(),
            "Password reset completed");
        log.info("Password reset for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        OtpCode otp = otpCodeRepository
            .findTopByEmailAndPurposeAndUsedFalseOrderByGeneratedAtDesc(request.getEmail(), request.getPurpose())
            .orElseThrow(() -> new TokenException("OTP not found or already used"));

        if (!otp.getCode().equals(request.getOtp())) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpCodeRepository.save(otp);
            throw new TokenException("Invalid OTP");
        }

        if (otp.isExpired()) {
            throw new TokenException("OTP has expired. Please request a new one.");
        }

        otp.setUsed(true);
        otpCodeRepository.save(otp);
        auditService.log(AuditAction.OTP_VERIFIED, "User", request.getEmail(), "OTP verified");
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.isEnabled()) {
            throw new BusinessException("Account is already verified");
        }

        verificationTokenRepository.deleteByUserId(user.getId());
        String token = UUID.randomUUID().toString();
        verificationTokenRepository.save(VerificationToken.builder()
            .token(token)
            .user(user)
            .expiryDate(LocalDateTime.now().plusHours(24))
            .build());

        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), token);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setForcePasswordChange(false);
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId());
        auditService.log(AuditAction.PASSWORD_CHANGE, "User", user.getId().toString(),
            "Password changed successfully");
        log.info("Password changed for user: {}", email);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        Set<String> roles = user.getRoles().stream()
            .map(Role::getName).collect(Collectors.toSet());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(jwtService.getAccessTokenExpiration())
            .user(UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .enabled(user.isEnabled())
                .forcePasswordChange(user.isForcePasswordChange())
                .roles(roles)
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build())
            .build();
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(1000000));
    }
}
