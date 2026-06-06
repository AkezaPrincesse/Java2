package com.exam.utility.dto.response.meter;

import com.exam.utility.enums.MeterStatus;
import com.exam.utility.enums.MeterType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MeterResponse {
    private Long id;
    private String meterNumber;
    private MeterType meterType;
    private MeterStatus status;
    private LocalDate installationDate;
    private String location;
    private BigDecimal initialReading;
    private Long customerId;
    private String customerName;
    private Long tariffId;
    private String tariffName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
