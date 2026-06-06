package com.exam.utility.dto.response.billing;

import com.exam.utility.enums.BillStatus;
import com.exam.utility.enums.MeterType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BillResponse {
    private Long id;
    private String billNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Long meterId;
    private String meterNumber;
    private MeterType utilityType;
    private Integer billingYear;
    private Integer billingMonth;
    private Double consumption;
    private BigDecimal consumptionAmount;
    private BigDecimal serviceChargeAmount;
    private BigDecimal taxAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private BillStatus status;
    private LocalDate billDate;
    private LocalDate dueDate;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private LocalDateTime paidAt;
    private String notes;
    private List<BillItemResponse> items;
    private LocalDateTime createdAt;
}
