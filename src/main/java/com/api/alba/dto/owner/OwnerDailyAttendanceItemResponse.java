package com.api.alba.dto.owner;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OwnerDailyAttendanceItemResponse {
    private Long userId;
    private String userName;
    private String profileColor;
    private String profileInitial;
    private String status;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private Integer workedMinutes;
    private BigDecimal finalWage;
    private String wageType;
    private BigDecimal monthlyWage;
    private BigDecimal dailyWage;
}
