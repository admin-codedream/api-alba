package com.api.alba.dto.owner;

import lombok.Getter;

import javax.validation.constraints.DecimalMin;
import java.math.BigDecimal;

@Getter
public class UpdateMemberHourlyWageRequest {
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal hourlyWage;
}