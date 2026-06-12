package com.api.alba.dto.owner;

import lombok.Getter;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Getter
public class UpdateEmployeeInsuranceSettingRequest {

    @NotNull
    private Boolean useNationalPension;

    @NotNull
    private Boolean useHealthInsurance;

    @NotNull
    private Boolean useLongTermCare;

    @NotNull
    private Boolean useEmploymentInsurance;

    @NotNull
    private Boolean useIncomeTax;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal taxFreeAmount;
}