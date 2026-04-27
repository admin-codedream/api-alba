package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Getter
@Setter
public class UpdateSalaryCalcUnitRequest {
    @NotBlank(message = "급여 계산 단위를 입력해 주세요.")
    @Pattern(
            regexp = "MINUTE|10MIN|HOUR",
            message = "급여 계산 단위가 올바르지 않아요."
    )
    private String salaryCalcUnit;
}
