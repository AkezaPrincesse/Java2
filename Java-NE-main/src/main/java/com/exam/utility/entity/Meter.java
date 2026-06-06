package com.exam.utility.entity;

import com.exam.utility.enums.MeterStatus;
import com.exam.utility.enums.MeterType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meters")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Meter extends BaseEntity {

    @Column(unique = true, nullable = false, length = 50)
    private String meterNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeterType meterType;

    @Column(nullable = false)
    private LocalDate installationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MeterStatus status = MeterStatus.ACTIVE;

    @Column(length = 255)
    private String location;

    @Column(precision = 10, scale = 2)
    private BigDecimal initialReading;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tariff_id")
    private Tariff tariff;

    @OneToMany(mappedBy = "meter", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MeterReading> readings = new ArrayList<>();
}
