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
    private BigDecimal appliedRate;       // 적용 요율 (자동 계산 시에만 저장)
    private BigDecimal appliedBaseAmount; // 계산 기준 금액 (자동 계산 시에만 저장)
}