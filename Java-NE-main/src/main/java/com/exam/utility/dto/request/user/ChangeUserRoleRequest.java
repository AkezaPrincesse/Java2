package com.exam.utility.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request payload for changing an existing user's role. */
@Data
public class ChangeUserRoleRequest {

    @NotBlank(message = "Role is required")
    private String role;
}
