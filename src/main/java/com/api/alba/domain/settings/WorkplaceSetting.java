package com.api.alba.domain.settings;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
public class WorkplaceSetting {
    private Long id;
    private Long workplaceId;
    private Integer lateGraceMinutes;
    private String salaryCalcUnit;
    private String roundingPolicy;
    private BigDecimal defaultHourlyWage;
    private Boolean useBreakPolicy;
    private LocalTime defaultCheckInTime;
    private LocalTime defaultCheckOutTime;
}
