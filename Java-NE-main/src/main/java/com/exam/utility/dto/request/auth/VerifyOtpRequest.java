package com.exam.utility.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits")
    private String otp;

    /**
     * Optional — defaults to PASSWORD_RESET when not provided.
     * Callers only need to supply this when verifying an OTP for a non-default purpose.
     */
    private String purpose = "PASSWORD_RESET";
}
