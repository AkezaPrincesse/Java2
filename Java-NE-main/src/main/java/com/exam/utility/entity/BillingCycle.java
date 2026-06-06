package com.exam.utility.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "billing_cycles",
    uniqueConstraints = @UniqueConstraint(columnNames = {"billing_year", "billing_month"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillingCycle extends BaseEntity {

    @Column(nullable = false)
    private Integer billingYear;

    @Column(nullable = false)
    private Integer billingMonth;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean closed = false;

    @OneToMany(mappedBy = "billingCycle", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Bill> bills = new ArrayList<>();
}
