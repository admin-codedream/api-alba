package com.api.alba.component;

import com.api.alba.domain.owner.EmployeeInsuranceSetting;
import com.api.alba.domain.owner.InsuranceRateRule;
import com.api.alba.domain.owner.PayslipDeduction;
import com.api.alba.mapper.owner.InsuranceRateRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 4대보험 공제액 자동 계산 헬퍼
 *
 * 계산 순서:
 *  1. 과세급여 = 총 지급액 - 비과세 금액
 *  2. 국민연금 = 과세급여 × 국민연금 근로자 요율 (원 단위 절사)
 *  3. 건강보험 = 과세급여 × 건강보험 근로자 요율 (원 단위 절사)
 *  4. 장기요양 = 건강보험 공제액 × 장기요양 근로자 요율 (원 단위 절사)
 *     (BASE_TYPE = HEALTH_INSURANCE_AMOUNT인 경우)
 *     (BASE_TYPE = TAXABLE_WAGE인 경우 과세급여 기준)
 *  5. 고용보험 = 과세급여 × 고용보험 근로자 요율 (원 단위 절사)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsuranceCalculationHelper {

    private final InsuranceRateRuleMapper insuranceRateRuleMapper;

    /**
     * 4대보험 공제 항목 목록 계산
     *
     * @param setting      직원별 보험 적용 설정
     * @param totalPay     총 지급액 (기본급 + 주휴수당 + 추가지급)
     * @param baseDate     적용 기준일 (급여 귀속월 말일)
     * @return 자동 계산된 공제 항목 목록 (적용 보험이 없으면 빈 리스트)
     */
    public List<PayslipDeduction> calculate(EmployeeInsuranceSetting setting, BigDecimal totalPay, LocalDate baseDate) {
        List<PayslipDeduction> deductions = new ArrayList<>();

        boolean anyEnabled = setting.isUseNationalPension()
                || setting.isUseHealthInsurance()
                || setting.isUseLongTermCare()
                || setting.isUseEmploymentInsurance();
        if (!anyEnabled) {
            return deductions;
        }

        BigDecimal taxFree = setting.getTaxFreeAmount() != null ? setting.getTaxFreeAmount() : BigDecimal.ZERO;
        BigDecimal taxableWage = totalPay.subtract(taxFree);
        if (taxableWage.compareTo(BigDecimal.ZERO) <= 0) {
            return deductions;
        }

        List<InsuranceRateRule> rates = insuranceRateRuleMapper.findActiveEmployeeRates(baseDate);
        Map<String, InsuranceRateRule> rateMap = rates.stream()
                .collect(Collectors.toMap(InsuranceRateRule::getInsuranceType, r -> r, (a, b) -> a));

        int order = 1;
        BigDecimal healthInsuranceAmount = BigDecimal.ZERO;

        // 국민연금
        if (setting.isUseNationalPension()) {
            InsuranceRateRule rule = rateMap.get("NATIONAL_PENSION");
            if (rule != null) {
                BigDecimal amount = floorToWon(taxableWage.multiply(rule.getRate()));
                deductions.add(buildDeduction("NATIONAL_PENSION", "국민연금", amount, taxableWage, rule.getRate(), order++));
            }
        }

        // 건강보험
        if (setting.isUseHealthInsurance()) {
            InsuranceRateRule rule = rateMap.get("HEALTH_INSURANCE");
            if (rule != null) {
                healthInsuranceAmount = floorToWon(taxableWage.multiply(rule.getRate()));
                deductions.add(buildDeduction("HEALTH_INSURANCE", "건강보험", healthInsuranceAmount, taxableWage, rule.getRate(), order++));
            }
        }

        // 장기요양보험
        if (setting.isUseLongTermCare()) {
            InsuranceRateRule rule = rateMap.get("LONG_TERM_CARE");
            if (rule != null) {
                BigDecimal base;
                if ("HEALTH_INSURANCE_AMOUNT".equals(rule.getBaseType()) && healthInsuranceAmount.compareTo(BigDecimal.ZERO) > 0) {
                    base = healthInsuranceAmount;
                } else {
                    base = taxableWage;
                }
                BigDecimal amount = floorToWon(base.multiply(rule.getRate()));
                deductions.add(buildDeduction("LONG_TERM_CARE", "장기요양보험", amount, base, rule.getRate(), order++));
            }
        }

        // 고용보험
        if (setting.isUseEmploymentInsurance()) {
            InsuranceRateRule rule = rateMap.get("EMPLOYMENT_INSURANCE");
            if (rule != null) {
                BigDecimal amount = floorToWon(taxableWage.multiply(rule.getRate()));
                deductions.add(buildDeduction("EMPLOYMENT_INSURANCE", "고용보험", amount, taxableWage, rule.getRate(), order++));
            }
        }

        return deductions;
    }

    private PayslipDeduction buildDeduction(String type, String name, BigDecimal amount,
                                             BigDecimal baseAmount, BigDecimal rate, int order) {
        PayslipDeduction d = new PayslipDeduction();
        d.setDeductionType(type);
        d.setName(name);
        d.setAmount(amount);
        d.setAppliedRate(rate);
        d.setAppliedBaseAmount(baseAmount);
        d.setDisplayOrder(order);
        return d;
    }

    /** 원 단위 절사 */
    private BigDecimal floorToWon(BigDecimal value) {
        return value.setScale(0, RoundingMode.FLOOR);
    }
}