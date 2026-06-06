package com.exam.utility.dto.request.billing;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApproveBillRequest {

    @NotBlank(message = "Bill number is required")
    private String billNumber;

    private String notes;
}
