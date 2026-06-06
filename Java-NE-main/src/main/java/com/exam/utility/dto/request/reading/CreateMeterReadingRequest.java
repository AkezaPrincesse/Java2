package com.exam.utility.dto.request.reading;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateMeterReadingRequest {

    @NotNull(message = "Meter ID is required")
    @Positive(message = "Meter ID must be positive")
    private Long meterId;

    @NotNull(message = "Previous reading is required")
    @PositiveOrZero(message = "Previous reading must be zero or positive")
    private Double previousReading;

    @NotNull(message = "Current reading is required")
    @PositiveOrZero(message = "Current reading must be zero or positive")
    private Double currentReading;

    @NotNull(message = "Reading date is required")
    @PastOrPresent(message = "Reading date cannot be in the future")
    private LocalDate readingDate;

    @Size(max = 255)
    private String notes;
}
