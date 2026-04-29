package com.api.alba.dto.owner;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PayslipDeductionItemResponse {
    private Long id;
    private String deductionType;
    private String name;
    private BigDecimal amount;
    private String note;
    private int displayOrder;
}