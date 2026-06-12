package com.api.alba.dto.owner;

import com.api.alba.domain.owner.EmployeeInsuranceSetting;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class EmployeeInsuranceSettingResponse {
    private final boolean useNationalPension;
    private final boolean useHealthInsurance;
    private final boolean useLongTermCare;
    private final boolean useEmploymentInsurance;
    private final boolean useIncomeTax;
    private final BigDecimal taxFreeAmount;

    public EmployeeInsuranceSettingResponse(EmployeeInsuranceSetting setting) {
        this.useNationalPension = setting.isUseNationalPension();
        this.useHealthInsurance = setting.isUseHealthInsurance();
        this.useLongTermCare = setting.isUseLongTermCare();
        this.useEmploymentInsurance = setting.isUseEmploymentInsurance();
        this.useIncomeTax = setting.isUseIncomeTax();
        this.taxFreeAmount = setting.getTaxFreeAmount();
    }
}