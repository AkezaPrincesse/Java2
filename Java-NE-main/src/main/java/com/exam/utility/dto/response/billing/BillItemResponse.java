package com.exam.utility.dto.response.billing;

import com.exam.utility.enums.BillItemType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BillItemResponse {
    private Long id;
    private BillItemType itemType;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}
