package com.api.alba.domain.log;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApiErrorLog {
    private Long id;
    private String requestUri;
    private String httpMethod;
    private String controller;
    private Long userId;
    private Long workplaceId;
    private String requestParams;
    private String requestHeaders;
    private String errorMessage;
    private String clientIp;
    private String userAgent;
    private LocalDateTime createdAt;
}