package com.api.alba.domain.staff;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WorkplaceMember {
    private Long id;
    private Long workplaceId;
    private Long userId;
    private String role;
    private String wageType;
    private BigDecimal hourlyWage;
    private BigDecimal monthlyWage;
    private BigDecimal dailyWage;
    private String memo;
    private Boolean receiveAttendancePush;
    private String status;
    private Integer breakMinutes;
    private Boolean useWeeklyHolidayPay;
}
