package com.api.alba.dto.owner;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class CalculateDeductionsResponse {

    private final List<DeductionItem> deductions;
    private final BigDecimal totalDeduction;

    @Getter
    @AllArgsConstructor
    public static class DeductionItem {
        private final String deductionType;
        private final String name;
        private final BigDecimal amount;
        private final BigDecimal appliedRate;
        private final BigDecimal appliedBaseAmount;
        private final int displayOrder;
    }
}