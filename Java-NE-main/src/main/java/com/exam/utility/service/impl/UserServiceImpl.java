package com.exam.utility.service.impl;

import com.exam.utility.dto.request.user.ChangeUserRoleRequest;
import com.exam.utility.dto.request.user.CreateUserRequest;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.auth.UserResponse;
import com.exam.utility.entity.Role;
import com.exam.utility.entity.User;
import com.exam.utility.enums.AuditAction;
import com.exam.utility.exception.BusinessException;
import com.exam.utility.exception.DuplicateResourceException;
import com.exam.utility.exception.ResourceNotFoundException;
import com.exam.utility.repository.RoleRepository;
import com.exam.utility.repository.UserRepository;
import com.exam.utility.service.AuditService;
import com.exam.utility.service.EmailService;
import com.exam.utility.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles administrator-driven user account creation and lifecycle management.
 *
 * Business rules enforced:
 * - Email must be unique across all users.
 * - A temporary password is auto-generated and emailed; the user must change it on first login.
 * - Newly created accounts are set to ACTIVE and forcePasswordChange = true.
 * - Role changes are audit-logged and the user is notified by email.
 * - Deactivation is a soft operation (enabled = false); accounts are never deleted.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private static final String TEMP_PASSWORD_CHARS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@$!%*?&";

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Role role = roleRepository.findByName(request.getRole())
            .orElseThrow(() -> new ResourceNotFoundException("Role", "name", request.getRole()));

        String temporaryPassword = generateTemporaryPassword();

        User user = User.builder()
            .fullName(request.getFullName())
            .email(request.getEmail().toLowerCase())
            .password(passwordEncoder.encode(temporaryPassword))
            .phoneNumber(request.getPhoneNumber())
            .enabled(true)           // admin-created accounts are immediately active
            .forcePasswordChange(true) // user must change password on first login
            .roles(Set.of(role))
            .build();

        user = userRepository.save(user);

        emailService.sendUserCreatedEmail(
            user.getEmail(),
            user.getFullName(),
            temporaryPassword,
            role.getName(),
            frontendUrl + "/login"
        );

        auditService.log(AuditAction.CREATE, "User", user.getId().toString(),
            "Admin created user: " + user.getEmail() + " with role: " + role.getName());

        log.info("Admin created user: {} with role: {}", user.getEmail(), role.getName());
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(Pageable pageable) {
        return PagedResponse.of(userRepository.findAll(pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return toResponse(userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id)));
    }

    @Override
    @Transactional
    public UserResponse changeUserRole(Long userId, ChangeUserRoleRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String previousRole = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.joining(", "));

        Role newRole = roleRepository.findByName(request.getRole())
            .orElseThrow(() -> new ResourceNotFoundException("Role", "name", request.getRole()));

        if (previousRole.equals(newRole.getName())) {
            throw new BusinessException("User already has the role: " + request.getRole());
        }

        user.getRoles().clear();
        user.getRoles().add(newRole);
        user = userRepository.save(user);

        // Notify user of role change with description of new permissions
        String permissionDescription = newRole.getDescription() != null
            ? newRole.getDescription()
            : "Role: " + newRole.getName();

        emailService.sendRoleChangedEmail(
            user.getEmail(),
            user.getFullName(),
            previousRole,
            newRole.getName(),
            permissionDescription
        );

        auditService.log(AuditAction.ROLE_ASSIGNED, "User", user.getId().toString(),
            "Role changed from [" + previousRole + "] to [" + newRole.getName() + "] for user: " + user.getEmail());

        log.info("Role changed for user {}: {} -> {}", user.getEmail(), previousRole, newRole.getName());
        return toResponse(user);
    }

    @Override
    @Transactional
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.isEnabled()) {
            throw new BusinessException("User account is already active");
        }

        user.setEnabled(true);
        userRepository.save(user);
        auditService.log(AuditAction.UPDATE, "User", userId.toString(),
            "User account activated: " + user.getEmail());
        log.info("User activated: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!user.isEnabled()) {
            throw new BusinessException("User account is already inactive");
        }

        user.setEnabled(false);
        userRepository.save(user);
        auditService.log(AuditAction.DEACTIVATE, "User", userId.toString(),
            "User account deactivated: " + user.getEmail());
        log.info("User deactivated: {}", user.getEmail());
    }

    /**
     * Generates a cryptographically secure temporary password that satisfies the password policy:
     * at least one uppercase, lowercase, digit, and special character, minimum 12 chars total.
     */
    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        // Guarantee policy compliance by seeding with one char from each required category
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "@$!%*?&";

        StringBuilder password = new StringBuilder();
        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // Fill remaining 8 characters from the full allowed set
        for (int i = 4; i < 12; i++) {
            password.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
        }

        // Shuffle to avoid predictable prefix pattern
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> searchUsers(String keyword, Pageable pageable) {
        return PagedResponse.of(userRepository.searchUsers(keyword, pageable).map(this::toResponse));
    }

    private UserResponse toResponse(User user) {
        Set<String> roles = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());

        return UserResponse.builder()
            .id(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .phoneNumber(user.getPhoneNumber())
            .enabled(user.isEnabled())
            .forcePasswordChange(user.isForcePasswordChange())
            .roles(roles)
            .lastLogin(user.getLastLogin())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
