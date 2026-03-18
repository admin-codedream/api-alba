package com.api.alba.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EmployeeWageSummary {
    private Long userId;
    private String userName;
    private Integer totalWorkedMinutes;
    private BigDecimal totalExpectedWage;
}
