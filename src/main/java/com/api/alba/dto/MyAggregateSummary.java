package com.api.alba.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MyAggregateSummary {
    private Integer totalWorkedMinutes;
    private BigDecimal totalExpectedWage;
}
