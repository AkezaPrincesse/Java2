package com.exam.utility.service;

import com.exam.utility.dto.request.auth.*;
import com.exam.utility.dto.response.auth.AuthResponse;
import com.exam.utility.dto.response.auth.TokenRefreshResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);
    TokenRefreshResponse refreshToken(RefreshTokenRequest request);
    void logout(HttpServletRequest request);
    void verifyEmail(String token);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    void verifyOtp(VerifyOtpRequest request);
    void resendVerificationEmail(String email);
    /** Handles the mandatory first-login password change for admin-created accounts. */
    void changePassword(ChangePasswordRequest request);
}
