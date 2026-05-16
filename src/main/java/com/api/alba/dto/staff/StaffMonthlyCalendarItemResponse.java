package com.api.alba.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class StaffMonthlyCalendarItemResponse {
    private LocalDate workDate;
    private Integer workedMinutes;
    private BigDecimal finalWage;
    private String wageType;
    private BigDecimal monthlyWage;
}
