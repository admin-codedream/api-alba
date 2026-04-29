package com.api.alba.domain.owner;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PayslipDeduction {
    private Long id;
    private Long payslipId;
    private String deductionType;
    private String name;
    private BigDecimal amount;
    private String note;
    private int displayOrder;
}