package com.api.alba.domain.owner;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class InsuranceRateRule {
    private Long id;
    private String insuranceType;  // NATIONAL_PENSION, HEALTH_INSURANCE, LONG_TERM_CARE, EMPLOYMENT_INSURANCE
    private String rateTarget;     // EMPLOYEE, EMPLOYER, TOTAL
    private BigDecimal rate;       // 요율 (예: 0.04750)
    private String baseType;       // TAXABLE_WAGE, HEALTH_INSURANCE_AMOUNT
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}