package com.api.alba.dto.staff;

import com.api.alba.dto.owner.PayslipDeductionItemResponse;
import com.api.alba.dto.owner.PayslipRecordItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class StaffPayslipDetailResponse {
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
    private BigDecimal weeklyHolidayPay;
    private BigDecimal bonusAmount;
    private BigDecimal deductionAmount;
    private BigDecimal totalWage;
    private String bonusNote;
    private List<PayslipDeductionItemResponse> deductions;
    private List<PayslipRecordItem> records;
}