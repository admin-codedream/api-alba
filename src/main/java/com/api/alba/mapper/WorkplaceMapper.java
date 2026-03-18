package com.api.alba.mapper;

import com.api.alba.domain.Workplace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkplaceMapper {
    int insert(Workplace workplace);

    Workplace findById(@Param("id") Long id);
}
