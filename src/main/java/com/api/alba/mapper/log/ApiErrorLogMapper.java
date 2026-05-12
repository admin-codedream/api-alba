package com.api.alba.mapper.log;

import com.api.alba.domain.log.ApiErrorLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApiErrorLogMapper {
    int insert(ApiErrorLog apiErrorLog);
}