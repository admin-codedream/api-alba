package com.api.alba.mapper.owner;

import com.api.alba.domain.owner.InsuranceRateRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface InsuranceRateRuleMapper {

    /**
     * 기준일자에 유효한 근로자 부담 보험 요율 전체 조회
     */
    List<InsuranceRateRule> findActiveEmployeeRates(@Param("baseDate") LocalDate baseDate);

    /**
     * 특정 보험 유형 + 대상의 기준일자 기준 요율 조회
     */
    InsuranceRateRule findRate(
            @Param("insuranceType") String insuranceType,
            @Param("rateTarget") String rateTarget,
            @Param("baseDate") LocalDate baseDate
    );

    List<InsuranceRateRule> findAll();
}