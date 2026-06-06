package com.exam.utility.dto.request.payment;

import com.exam.utility.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePaymentRequest {

    @NotBlank(message = "Bill number is required")
    private String billNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Payment amount must be at least 1 RWF")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Size(max = 100)
    private String transactionReference;

    @Size(max = 100)
    private String paidBy;

    @Size(max = 500)
    private String notes;
}
