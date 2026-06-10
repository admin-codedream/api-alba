package com.api.alba.dto.owner;

import lombok.Getter;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Getter
public class UpdateMemberWageRequest {
    @NotBlank(message = "급여 유형을 입력해 주세요.")
    private String wageType; // HOURLY, MONTHLY or DAILY

    @DecimalMin(value = "0.0", inclusive = false, message = "시급은 0보다 커야 해요.")
    private BigDecimal hourlyWage;

    @DecimalMin(value = "0.0", inclusive = false, message = "월급은 0보다 커야 해요.")
    private BigDecimal monthlyWage;

    @DecimalMin(value = "0.0", inclusive = false, message = "일급은 0보다 커야 해요.")
    private BigDecimal dailyWage;
}