package com.exam.utility.dto.request.customer;

import com.exam.utility.enums.CustomerStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateCustomerRequest {

    @Size(min = 3, max = 100)
    private String fullName;

    @Email(message = "Email must be valid")
    private String email;

    @Pattern(regexp = "^(078|079|072|073)\\d{7}$",
             message = "Phone number must start with 078, 079, 072, or 073 and be exactly 10 digits")
    private String phoneNumber;

    @Size(max = 255)
    private String address;

    @Size(max = 100)
    private String district;

    @Size(max = 100)
    private String sector;

    private CustomerStatus status;
}
