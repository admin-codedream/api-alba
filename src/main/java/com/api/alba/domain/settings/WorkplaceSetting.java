package com.api.alba.domain.settings;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WorkplaceSetting {
    private Long id;
    private Long workplaceId;
    private Integer lateGraceMinutes;
    private String salaryCalcUnit;
    private String roundingPolicy;
    private BigDecimal defaultHourlyWage;
}
