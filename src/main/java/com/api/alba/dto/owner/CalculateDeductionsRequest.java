package com.api.alba.dto.owner;

import lombok.Getter;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Getter
public class CalculateDeductionsRequest {

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal bonusAmount;
}