package com.api.alba.domain.owner;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class EmployeeInsuranceSetting {
    private Long id;
    private Long workplaceMemberId;
    private boolean useNationalPension;
    private boolean useHealthInsurance;
    private boolean useLongTermCare;
    private boolean useEmploymentInsurance;
    private boolean useIncomeTax;
    private BigDecimal taxFreeAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}