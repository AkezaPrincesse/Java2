package com.exam.utility.dto.response.customer;

import com.exam.utility.enums.CustomerStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CustomerResponse {
    private Long id;
    private String fullName;
    private String nationalId;
    private String email;
    private String phoneNumber;
    private String address;
    private String district;
    private String sector;
    private CustomerStatus status;
    private int totalMeters;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
