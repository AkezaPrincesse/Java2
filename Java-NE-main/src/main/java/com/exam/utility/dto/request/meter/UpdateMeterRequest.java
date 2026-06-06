package com.exam.utility.dto.request.meter;

import com.exam.utility.enums.MeterStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateMeterRequest {

    private MeterStatus status;
    private Long tariffId;
    private LocalDate installationDate;

    @Size(max = 255)
    private String location;
}
