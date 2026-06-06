package com.exam.utility.entity;

import com.exam.utility.enums.MeterType;
import com.exam.utility.enums.TaxType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "taxes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tax extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaxType taxType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeterType utilityType;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal rate;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean appliedToConsumption = true;
}
