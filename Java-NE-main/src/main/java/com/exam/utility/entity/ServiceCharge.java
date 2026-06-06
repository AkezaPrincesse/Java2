package com.exam.utility.entity;

import com.exam.utility.enums.MeterType;
import com.exam.utility.enums.ServiceChargeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "service_charges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceCharge extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceChargeType chargeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeterType utilityType;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
