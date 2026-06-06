package com.exam.utility.dto.request.customer;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateCustomerRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    private String fullName;

    @NotBlank(message = "National ID is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "National ID must be 16 digits")
    private String nationalId;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(078|079|072|073)\\d{7}$",
             message = "Phone number must start with 078, 079, 072, or 073 and be exactly 10 digits")
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    @Size(max = 255)
    private String address;

    @Size(max = 100)
    private String district;

    @Size(max = 100)
    private String sector;
}
