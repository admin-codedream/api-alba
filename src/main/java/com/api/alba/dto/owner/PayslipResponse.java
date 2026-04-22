package com.api.alba.dto.owner;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class PayslipResponse {
    private String staffName;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<PayslipDailyItemResponse> dailyList;
    private int totalWorkDays;
    private int totalWorkedMinutes;
    private BigDecimal totalWage;
}
