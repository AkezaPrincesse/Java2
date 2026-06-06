package com.exam.utility.dto.response.tariff;

import com.exam.utility.enums.MeterType;
import com.exam.utility.enums.TariffType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TariffResponse {
    private Long id;
    private String name;
    private String description;
    private MeterType utilityType;
    private TariffType tariffType;
    private BigDecimal flatRate;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private boolean active;
    private List<TariffVersionResponse> versions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
