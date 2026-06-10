package com.api.alba.dto.attendance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class AutoCheckOutTarget {
    private Long recordId;
    private Long workplaceId;
    private Long userId;
    private LocalDateTime checkInAt;
    private LocalTime scheduledCheckInTime;
    private LocalTime scheduledCheckOutTime;
    private String wageType;
    private BigDecimal hourlyWage;
    private BigDecimal monthlyWage;
    private BigDecimal dailyWage;
    private Integer breakMinutes;
    /** 야간 근무 여부 - true이면 checkOutAt = workDate + 1일 + scheduledCheckOutTime */
    private boolean overnight;
}