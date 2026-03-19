package com.api.alba.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class StaffTodaySummaryResponse {
    private Long workplaceId;
    private LocalDate workDate;
    private Integer todayWorkedMinutes;
    private BigDecimal todayExpectedWage;
    private Integer cumulativeWorkedMinutes;
    private BigDecimal cumulativeExpectedWage;
}
