package com.exam.utility.dto.request.billing;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request body for batch bill generation.
 * Business rule: billing period must not be in the future — enforced in BillingEngine.
 */
@Data
public class GenerateBillsRequest {

    @NotNull(message = "Billing year is required")
    @Min(value = 2020, message = "Billing year must be 2020 or later")
    @Max(value = 2100, message = "Billing year must not exceed 2100")
    private Integer billingYear;

    @NotNull(message = "Billing month is required")
    @Min(value = 1, message = "Billing month must be between 1 and 12")
    @Max(value = 12, message = "Billing month must be between 1 and 12")
    private Integer billingMonth;

    private Long customerId;
    private Long meterId;
}
