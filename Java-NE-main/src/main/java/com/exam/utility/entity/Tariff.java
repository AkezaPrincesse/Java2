package com.exam.utility.entity;

import com.exam.utility.enums.MeterType;
import com.exam.utility.enums.TariffType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tariffs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tariff extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeterType utilityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TariffType tariffType;

    @Column(precision = 15, scale = 4)
    private BigDecimal flatRate;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    private LocalDate expiryDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "tariff", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TariffVersion> versions = new ArrayList<>();

    @OneToMany(mappedBy = "tariff")
    @Builder.Default
    private List<Meter> meters = new ArrayList<>();
}
