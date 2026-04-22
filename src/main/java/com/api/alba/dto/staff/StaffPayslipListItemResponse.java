package com.api.alba.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class StaffPayslipListItemResponse {
    private Long payslipId;
    private Long workplaceId;
    private String workplaceName;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDate issuedAt;
    private int workedDays;
    private int workedMinutes;
    private BigDecimal hourlyWage;
    private BigDecimal baseWage;
    private BigDecimal bonusAmount;
    private BigDecimal deductionAmount;
    private BigDecimal totalWage;
}
