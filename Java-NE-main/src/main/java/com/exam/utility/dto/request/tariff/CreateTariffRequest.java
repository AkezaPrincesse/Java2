package com.exam.utility.dto.request.tariff;

import com.exam.utility.enums.MeterType;
import com.exam.utility.enums.TariffType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateTariffRequest {

    @NotBlank(message = "Tariff name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    @NotNull(message = "Utility type is required")
    private MeterType utilityType;

    @NotNull(message = "Tariff type is required")
    private TariffType tariffType;

    @DecimalMin(value = "0.0", inclusive = false, message = "Flat rate must be positive")
    private BigDecimal flatRate;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    private LocalDate expiryDate;

    private List<TariffTierRequest> tiers;
}
