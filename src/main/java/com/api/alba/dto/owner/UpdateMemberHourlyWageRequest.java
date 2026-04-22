package com.api.alba.dto.owner;

import lombok.Getter;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Getter
public class UpdateMemberHourlyWageRequest {
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal hourlyWage;
}