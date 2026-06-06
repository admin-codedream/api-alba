package com.api.alba.dto.owner;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class OwnerMonthlyCalendarItemResponse {
    private Long userId;
    private String userName;
    private String profileColor;
    private String profileInitial;
    private LocalDate workDate;
    private BigDecimal workedHours;
    private BigDecimal finalWage;
}
