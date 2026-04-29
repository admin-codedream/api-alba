package com.api.alba.dto.owner;

import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Getter
public class UpdatePayslipRequest {
    @NotNull
    private BigDecimal bonusAmount;

    private String bonusNote;
}