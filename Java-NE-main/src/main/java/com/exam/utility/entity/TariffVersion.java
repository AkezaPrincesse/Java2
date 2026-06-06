package com.exam.utility.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tariff_versions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TariffVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tariff_id", nullable = false)
    private Tariff tariff;

    @Column(nullable = false)
    private Integer tierOrder;

    @Column(precision = 15, scale = 4)
    private BigDecimal minUnits;

    @Column(precision = 15, scale = 4)
    private BigDecimal maxUnits;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal ratePerUnit;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    private LocalDate expiryDate;

    @Column(length = 255)
    private String description;
}
