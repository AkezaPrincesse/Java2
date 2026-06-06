package com.exam.utility.dto.response.reading;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MeterReadingResponse {
    private Long id;
    private Long meterId;
    private String meterNumber;
    private String meterType;
    private String customerName;
    private BigDecimal previousReading;
    private BigDecimal currentReading;
    private BigDecimal consumption;
    private LocalDate readingDate;
    private Integer readingYear;
    private Integer readingMonth;
    private String notes;
    private LocalDateTime createdAt;
    private String createdBy;
}
