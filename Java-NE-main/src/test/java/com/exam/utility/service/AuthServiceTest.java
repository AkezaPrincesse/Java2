package com.exam.utility.service;

import com.exam.utility.dto.request.auth.RegisterRequest;
import com.exam.utility.dto.response.auth.AuthResponse;
import com.exam.utility.entity.Role;
import com.exam.utility.entity.User;
import com.exam.utility.entity.VerificationToken;
import com.exam.utility.exception.DuplicateResourceException;
import com.exam.utility.exception.TokenException;
import com.exam.utility.repository.*;
import com.exam.utility.security.JwtService;
import com.exam.utility.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock VerificationTokenRepository verificationTokenRepository;
    @Mock OtpCodeRepository otpCodeRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock EmailService emailService;
    @Mock AuditService auditService;

    @InjectMocks AuthServiceImpl authService;

    private Role defaultRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        // id is in BaseEntity – set via setter after building
        defaultRole = Role.builder().name("ROLE_USER").build();
        defaultRole.setId(1L);

        testUser = User.builder()
            .fullName("Test User")
            .email("test@example.com")
            .password("encodedPass")
            .enabled(false)
            .roles(Set.of(defaultRole))
            .build();
        testUser.setId(1L);

        // inject the @Value field via reflection
        try {
            var field = AuthServiceImpl.class.getDeclaredField("refreshTokenExpiration");
            field.setAccessible(true);
            field.set(authService, 604800000L);
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Register – should create user and send verification email")
    void register_ShouldCreateUser_WhenEmailNotExists() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail("new@example.com");
        request.setPassword("Password@1");
        request.setPhoneNumber("+250788123456");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(verificationTokenRepository.save(any())).thenReturn(null);
        when(jwtService.getAccessTokenExpiration()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
        verify(emailService).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Register – should throw DuplicateResourceException when email already exists")
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setFullName("Test");
        request.setPassword("Password@1");
        request.setPhoneNumber("+250788000001");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("existing@example.com");
    }

    @Test
    @DisplayName("Verify email – should enable user when token is valid")
    void verifyEmail_ShouldEnableUser_WhenTokenIsValid() {
        VerificationToken token = new VerificationToken();
        token.setToken("valid-token");
        token.setUser(testUser);
        token.setUsed(false);
        token.setExpiryDate(LocalDateTime.now().plusHours(1));

        when(verificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.save(any())).thenReturn(testUser);
        when(verificationTokenRepository.save(any())).thenReturn(token);

        assertThatNoException().isThrownBy(() -> authService.verifyEmail("valid-token"));
        assertThat(testUser.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Verify email – should throw TokenException when token is expired")
    void verifyEmail_ShouldThrowException_WhenTokenExpired() {
        VerificationToken token = new VerificationToken();
        token.setToken("expired-token");
        token.setUser(testUser);
        token.setUsed(false);
        token.setExpiryDate(LocalDateTime.now().minusHours(1));

        when(verificationTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail("expired-token"))
            .isInstanceOf(TokenException.class)
            .hasMessageContaining("expired");
    }
}
