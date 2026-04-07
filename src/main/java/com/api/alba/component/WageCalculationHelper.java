package com.api.alba.component;

import com.api.alba.domain.settings.WorkplaceBreakPolicy;
import com.api.alba.domain.settings.WorkplaceSetting;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Component
public class WageCalculationHelper {
    private static final BigDecimal TEN_WON_UNIT = BigDecimal.TEN;

    public WageCalculationResult calculate(
            BigDecimal hourlyWage,
            int grossWorkedMinutes,
            WorkplaceSetting setting,
            List<WorkplaceBreakPolicy> breakPolicies
    ) {
        int payableWorkedMinutes = calculatePayableWorkedMinutes(grossWorkedMinutes, setting, breakPolicies);
        BigDecimal wage = calculateWage(hourlyWage, payableWorkedMinutes);
        return new WageCalculationResult(payableWorkedMinutes, wage, wage);
    }

    public int calculatePayableWorkedMinutes(
            int grossWorkedMinutes,
            WorkplaceSetting setting,
            List<WorkplaceBreakPolicy> breakPolicies
    ) {
        int normalizedWorkedMinutes = Math.max(grossWorkedMinutes, 0);
        int unpaidBreakMinutes = resolveUnpaidBreakMinutes(normalizedWorkedMinutes, setting, breakPolicies);
        return Math.max(normalizedWorkedMinutes - unpaidBreakMinutes, 0);
    }

    public BigDecimal calculateWage(BigDecimal hourlyWage, int workedMinutes) {
        BigDecimal resolvedHourlyWage = hourlyWage == null ? BigDecimal.ZERO : hourlyWage;
        BigDecimal wage = resolvedHourlyWage
                .multiply(BigDecimal.valueOf(Math.max(workedMinutes, 0)))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return truncateToTenWonUnit(wage);
    }

    private int resolveUnpaidBreakMinutes(
            int grossWorkedMinutes,
            WorkplaceSetting setting,
            List<WorkplaceBreakPolicy> breakPolicies
    ) {
        if (setting == null || !Boolean.TRUE.equals(setting.getUseBreakPolicy()) || breakPolicies == null || breakPolicies.isEmpty()) {
            return 0;
        }

        int autoBreakMinutes = breakPolicies.stream()
                .filter(policy -> Boolean.TRUE.equals(policy.getIsActive()))
                .filter(policy -> !Boolean.TRUE.equals(policy.getIsPaid()))
                .filter(policy -> "AUTO".equalsIgnoreCase(policy.getBreakType()))
                .filter(policy -> policy.getMinWorkMinutes() == null || grossWorkedMinutes >= policy.getMinWorkMinutes())
                .max(Comparator.comparingInt(policy -> policy.getMinWorkMinutes() == null ? 0 : policy.getMinWorkMinutes()))
                .map(WorkplaceBreakPolicy::getBreakMinutes)
                .orElse(0);

        int fixedBreakMinutes = breakPolicies.stream()
                .filter(policy -> Boolean.TRUE.equals(policy.getIsActive()))
                .filter(policy -> !Boolean.TRUE.equals(policy.getIsPaid()))
                .filter(policy -> "FIXED".equalsIgnoreCase(policy.getBreakType()))
                .map(WorkplaceBreakPolicy::getBreakMinutes)
                .filter(minutes -> minutes != null && minutes > 0)
                .mapToInt(Integer::intValue)
                .sum();

        return autoBreakMinutes + fixedBreakMinutes;
    }

    private BigDecimal truncateToTenWonUnit(BigDecimal wage) {
        if (wage == null) {
            return BigDecimal.ZERO;
        }
        return wage
                .divide(TEN_WON_UNIT, 0, RoundingMode.DOWN)
                .multiply(TEN_WON_UNIT)
                .setScale(2, RoundingMode.DOWN);
    }

    public record WageCalculationResult(
            int workedMinutes,
            BigDecimal baseWage,
            BigDecimal finalWage
    ) {
    }
}
