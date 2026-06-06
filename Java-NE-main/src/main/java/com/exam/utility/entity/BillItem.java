package com.exam.utility.entity;

import com.exam.utility.enums.BillItemType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "bill_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BillItemType itemType;

    @Column(nullable = false, length = 150)
    private String description;

    @Column(precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(precision = 15, scale = 4)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal amount;
}
