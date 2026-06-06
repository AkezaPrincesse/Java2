package com.exam.utility.dto.request.tariff;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TariffTierRequest {

    @NotNull @Positive
    private Integer tierOrder;

    @PositiveOrZero
    private BigDecimal minUnits;

    private BigDecimal maxUnits;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal ratePerUnit;

    @NotNull
    private LocalDate effectiveDate;

    @Size(max = 255)
    private String description;
}
