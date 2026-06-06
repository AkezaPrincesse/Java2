package com.exam.utility.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request payload for an Administrator creating a new internal staff account
 * (Operator, Finance Officer, Manager, etc.).
 * A secure temporary password is auto-generated; the admin does NOT supply it.
 */
@Data
public class CreateUserRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /**
     * Role name must match an existing role in the system, e.g. ROLE_OPERATOR, ROLE_FINANCE, ROLE_MANAGER.
     */
    @NotBlank(message = "Role is required")
    private String role;

    @Pattern(
        regexp = "^(078|079|072|073)\\d{7}$",
        message = "Phone number must start with 078, 079, 072, or 073 and be exactly 10 digits"
    )
    private String phoneNumber;
}
