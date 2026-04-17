package com.api.alba.mapper.settings;

import com.api.alba.domain.settings.WorkplaceSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalTime;

@Mapper
public interface WorkplaceSettingMapper {
    int insert(WorkplaceSetting workplaceSetting);

    WorkplaceSetting findByWorkplaceId(@Param("workplaceId") Long workplaceId);

    int updateDefaultHourlyWage(
            @Param("workplaceId") Long workplaceId,
            @Param("defaultHourlyWage") BigDecimal defaultHourlyWage
    );

    int updateUseBreakPolicy(
            @Param("workplaceId") Long workplaceId,
            @Param("useBreakPolicy") Boolean useBreakPolicy
    );

    int updateSalaryCalcUnit(
            @Param("workplaceId") Long workplaceId,
            @Param("salaryCalcUnit") String salaryCalcUnit
    );

    int updateDefaultWorkTime(
            @Param("workplaceId") Long workplaceId,
            @Param("defaultCheckInTime") LocalTime defaultCheckInTime,
            @Param("defaultCheckOutTime") LocalTime defaultCheckOutTime
    );
}
