package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Getter
@Setter
public class UpdateSalaryCalcUnitRequest {
    @NotBlank(message = "salaryCalcUnit is required.")
    @Pattern(
            regexp = "MINUTE|10MIN|HOUR",
            message = "salaryCalcUnit must be one of MINUTE, 10MIN, HOUR."
    )
    private String salaryCalcUnit;
}
