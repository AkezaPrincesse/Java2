package com.exam.utility.entity;

import com.exam.utility.enums.MeterType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "penalties")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Penalty extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeterType utilityType;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal rate;

    @Column(nullable = false)
    private Integer gracePeriodDays;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPercentage = true;

    @Column(precision = 15, scale = 4)
    private BigDecimal fixedAmount;
}
