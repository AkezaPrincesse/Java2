package com.exam.utility.dto.request.billing;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectBillRequest {

    @NotBlank(message = "Bill number is required")
    private String billNumber;

    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
