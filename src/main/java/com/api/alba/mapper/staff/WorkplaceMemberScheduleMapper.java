package com.api.alba.mapper.staff;

import com.api.alba.domain.staff.WorkplaceMemberSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkplaceMemberScheduleMapper {
    void insertAll(@Param("schedules") List<WorkplaceMemberSchedule> schedules);

    List<WorkplaceMemberSchedule> findByWorkplaceAndUser(
            @Param("workplaceId") Long workplaceId,
            @Param("userId") Long userId
    );

    void deleteByWorkplaceAndUser(
            @Param("workplaceId") Long workplaceId,
            @Param("userId") Long userId
    );
}