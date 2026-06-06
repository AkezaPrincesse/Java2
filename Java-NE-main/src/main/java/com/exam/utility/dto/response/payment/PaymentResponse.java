package com.exam.utility.dto.response.payment;

import com.exam.utility.enums.PaymentMethod;
import com.exam.utility.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private String receiptNumber;
    private String billNumber;
    private Long customerId;
    private String customerName;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime paymentDate;
    private String transactionReference;
    private String paidBy;
    private String notes;
    private LocalDateTime createdAt;
}
