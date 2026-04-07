package com.api.alba.component;

import com.api.alba.domain.settings.WorkplaceBreakPolicy;
import com.api.alba.domain.settings.WorkplaceSetting;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WageCalculationHelperTest {
    private final WageCalculationHelper wageCalculationHelper = new WageCalculationHelper();

    @Test
    void unpaidAutoBreakUsesHighestMatchedThreshold() {
        WorkplaceSetting setting = new WorkplaceSetting();
        setting.setUseBreakPolicy(true);

        WorkplaceBreakPolicy fourHoursBreak = breakPolicy("AUTO", 240, 30, false, true);
        WorkplaceBreakPolicy eightHoursBreak = breakPolicy("AUTO", 480, 60, false, true);

        int payableWorkedMinutes = wageCalculationHelper.calculatePayableWorkedMinutes(
                510,
                setting,
                List.of(fourHoursBreak, eightHoursBreak)
        );

        assertThat(payableWorkedMinutes).isEqualTo(450);
    }

    @Test
    void unpaidFixedBreaksAreSummedAndPaidBreaksIgnored() {
        WorkplaceSetting setting = new WorkplaceSetting();
        setting.setUseBreakPolicy(true);

        WorkplaceBreakPolicy fixedBreak1 = breakPolicy("FIXED", null, 15, false, true);
        WorkplaceBreakPolicy fixedBreak2 = breakPolicy("FIXED", null, 20, false, true);
        WorkplaceBreakPolicy paidBreak = breakPolicy("FIXED", null, 30, true, true);

        WageCalculationHelper.WageCalculationResult result = wageCalculationHelper.calculate(
                new BigDecimal("10000"),
                300,
                setting,
                List.of(fixedBreak1, fixedBreak2, paidBreak)
        );

        assertThat(result.workedMinutes()).isEqualTo(265);
        assertThat(result.finalWage()).isEqualByComparingTo("44160.00");
    }

    @Test
    void breakPolicyDisabledLeavesWorkedMinutesUntouched() {
        WorkplaceSetting setting = new WorkplaceSetting();
        setting.setUseBreakPolicy(false);

        WorkplaceBreakPolicy fixedBreak = breakPolicy("FIXED", null, 30, false, true);

        WageCalculationHelper.WageCalculationResult result = wageCalculationHelper.calculate(
                new BigDecimal("10000"),
                65,
                setting,
                List.of(fixedBreak)
        );

        assertThat(result.workedMinutes()).isEqualTo(65);
        assertThat(result.finalWage()).isEqualByComparingTo("10830.00");
    }

    private WorkplaceBreakPolicy breakPolicy(
            String breakType,
            Integer minWorkMinutes,
            Integer breakMinutes,
            boolean isPaid,
            boolean isActive
    ) {
        WorkplaceBreakPolicy policy = new WorkplaceBreakPolicy();
        policy.setBreakType(breakType);
        policy.setMinWorkMinutes(minWorkMinutes);
        policy.setBreakMinutes(breakMinutes);
        policy.setIsPaid(isPaid);
        policy.setIsActive(isActive);
        return policy;
    }
}
