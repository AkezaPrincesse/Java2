package com.exam.utility.dto.request.meter;

import com.exam.utility.enums.MeterType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateMeterRequest {

    @NotBlank(message = "Meter number is required")
    @Size(min = 5, max = 50, message = "Meter number must be between 5 and 50 characters")
    private String meterNumber;

    @NotNull(message = "Meter type is required")
    private MeterType meterType;

    @NotNull(message = "Installation date is required")
    @PastOrPresent(message = "Installation date cannot be in the future")
    private LocalDate installationDate;

    @NotNull(message = "Customer ID is required")
    @Positive(message = "Customer ID must be positive")
    private Long customerId;

    private Long tariffId;

    @Size(max = 255)
    private String location;

    @PositiveOrZero
    private Double initialReading;
}
