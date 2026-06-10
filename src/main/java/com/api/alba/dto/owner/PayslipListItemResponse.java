package com.api.alba.dto.owner;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class PayslipListItemResponse {
    private Long payslipId;
    private Long userId;
    private String userName;
    private String profileColor;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDate issuedAt;
    private int workedDays;
    private int workedMinutes;
    private String wageType;
    private BigDecimal hourlyWage;
    private BigDecimal monthlyWage;
    private BigDecimal dailyWage;
    private BigDecimal baseWage;
    private BigDecimal weeklyHolidayPay;
    private BigDecimal bonusAmount;
    private BigDecimal deductionAmount;
    private BigDecimal totalWage;
    private String status;
}
