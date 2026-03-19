package com.api.alba.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class StaffHomeTodayResponse {
    private Long workplaceId;
    private LocalDate workDate;
    private String attendanceStatus;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private Integer currentWorkedMinutes;
    private BigDecimal expectedWage;
    private BigDecimal hourlyWage;
}
