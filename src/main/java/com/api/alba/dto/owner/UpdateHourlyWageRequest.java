package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateHourlyWageRequest {
    @NotNull(message = "시급을 입력해 주세요.")
    @DecimalMin(value = "0.00", inclusive = true, message = "시급은 0 이상이어야 해요.")
    @Digits(integer = 8, fraction = 2, message = "시급 형식이 올바르지 않아요.")
    private BigDecimal hourlyWage;
}