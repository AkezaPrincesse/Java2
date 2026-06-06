package com.exam.utility.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "meter_readings",
    uniqueConstraints = @UniqueConstraint(columnNames = {"meter_id", "reading_year", "reading_month"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MeterReading extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meter_id", nullable = false)
    private Meter meter;

    @Column(nullable = false)
    private BigDecimal previousReading;

    @Column(nullable = false)
    private BigDecimal currentReading;

    @Column(nullable = false)
    private BigDecimal consumption;

    @Column(nullable = false)
    private LocalDate readingDate;

    @Column(nullable = false)
    private Integer readingYear;

    @Column(nullable = false)
    private Integer readingMonth;

    @Column(length = 255)
    private String notes;

    @Column(length = 255)
    private String readingImagePath;

    @PrePersist
    @PreUpdate
    private void calculateConsumption() {
        this.consumption = this.currentReading.subtract(this.previousReading);
        if (this.readingDate != null) {
            this.readingYear = this.readingDate.getYear();
            this.readingMonth = this.readingDate.getMonthValue();
        }
    }
}
