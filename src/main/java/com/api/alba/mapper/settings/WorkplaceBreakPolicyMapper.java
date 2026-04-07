package com.api.alba.mapper.settings;

import com.api.alba.domain.settings.WorkplaceBreakPolicy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkplaceBreakPolicyMapper {
    List<WorkplaceBreakPolicy> findAllByWorkplaceId(@Param("workplaceId") Long workplaceId);

    int insert(WorkplaceBreakPolicy policy);

    int deleteByWorkplaceId(@Param("workplaceId") Long workplaceId);
}