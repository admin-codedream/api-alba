package com.api.alba.dto.owner;

import lombok.Getter;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Getter
public class UpdatePayslipWithDeductionsRequest {

    @NotNull
    private BigDecimal bonusAmount;

    private String bonusNote;

    @NotNull
    @Valid
    private List<DeductionItem> deductions;

    @Getter
    public static class DeductionItem {
        @NotBlank
        private String deductionType;

        @NotBlank
        private String name;

        @NotNull
        @DecimalMin("0.0")
        private BigDecimal amount;

        private String note;

        private int displayOrder;
    }
}