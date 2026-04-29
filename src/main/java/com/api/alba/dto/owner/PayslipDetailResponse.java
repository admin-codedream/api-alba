package com.api.alba.dto.owner;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class PayslipDetailResponse {
    private Long payslipId;
    private Long userId;
    private String userName;
    private String profileColor;
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
    private String bonusNote;
    private List<PayslipDeductionItemResponse> deductions;
    private List<PayslipRecordItem> records;
}