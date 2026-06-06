package com.exam.utility.dto.response.auth;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private boolean enabled;
    private boolean forcePasswordChange;
    private Set<String> roles;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
}
