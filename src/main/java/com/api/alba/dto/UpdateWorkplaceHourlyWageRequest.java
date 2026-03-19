package com.api.alba.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Getter
@Setter
public class UpdateWorkplaceHourlyWageRequest {
    @NotNull(message = "hourlyWage is required.")
    @DecimalMin(value = "0.00", inclusive = true, message = "hourlyWage must be 0 or greater.")
    @Digits(integer = 8, fraction = 2, message = "hourlyWage must have up to 8 integer digits and 2 decimal places.")
    private BigDecimal hourlyWage;
}
