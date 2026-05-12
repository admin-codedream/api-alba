package com.api.alba.service.log;

import com.api.alba.domain.log.ApiErrorLog;
import com.api.alba.mapper.log.ApiErrorLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiErrorLogService {

    private final ApiErrorLogMapper apiErrorLogMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(ApiErrorLog errorLog) {
        try {
            apiErrorLogMapper.insert(errorLog);
        } catch (Exception e) {
            log.error("Failed to insert API error log", e);
        }
    }
}