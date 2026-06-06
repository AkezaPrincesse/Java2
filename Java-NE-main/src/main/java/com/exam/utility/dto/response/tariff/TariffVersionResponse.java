package com.exam.utility.dto.response.tariff;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class TariffVersionResponse {
    private Long id;
    private Integer tierOrder;
    private BigDecimal minUnits;
    private BigDecimal maxUnits;
    private BigDecimal ratePerUnit;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String description;
}
