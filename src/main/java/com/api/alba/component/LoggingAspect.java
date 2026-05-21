package com.api.alba.component;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {

    private final ObjectMapper objectMapper;

    @Pointcut("execution(* com.api.alba.controller..*Controller.*(..))")
    private static void advicePoint() {}

    @Around("advicePoint()")
    public Object logExecutionInfo(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        HttpServletRequest request = getCurrentHttpRequest();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String className = methodSignature.getDeclaringType().getSimpleName();
        String methodName = methodSignature.getName();
        String fullMethod = className + "." + methodName;
        String requestUri = request != null ? request.getRequestURI() : "N/A";
        String httpMethod = request != null ? request.getMethod() : "N/A";
        // 파라미터 이름과 값 매핑
        String[] paramNames = methodSignature.getParameterNames(); // 파라미터 이름
        Object[] paramValues = joinPoint.getArgs(); // 파라미터 값

        StringBuilder paramsLog = new StringBuilder();
        if (paramNames != null && paramValues != null) {
            for (int i = 0; i < paramNames.length; i++) {
                paramsLog.append(paramNames[i]).append("=")
                    .append(toJson(paramValues[i])).append(", ");
            }
        }

        log.info("[API] START  [{}] {} :: {} | params: {}", httpMethod, requestUri, fullMethod, paramsLog);

        Object result;

        try {
            result = joinPoint.proceed();  // 실제 컨트롤러 메서드 실행
        } catch (Throwable ex) {
            log.error("[API] ⛔ ERROR  [{}] {}  :: {} | message={}", httpMethod, requestUri, fullMethod, ex.getMessage());
            throw ex;
        }

        long end = System.currentTimeMillis();

        // END 로그
        log.info("[API] END   [{}] {} :: {} | duration={}ms", httpMethod, requestUri, fullMethod, (end - start));
        return result;
    }

    private String toJson(Object value) {
        if (value == null) return "null";
        try {
            JsonNode node = objectMapper.valueToTree(value);
            if (node.isObject()) {
                Set<String> maskedFields = getMaskedFieldNames(value.getClass());
                if (!maskedFields.isEmpty()) {
                    maskObjectNode((ObjectNode) node, maskedFields);
                }
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private Set<String> getMaskedFieldNames(Class<?> clazz) {
        Set<String> masked = new HashSet<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Masked.class)) {
                masked.add(field.getName());
            }
        }
        return masked;
    }

    private void maskObjectNode(ObjectNode node, Set<String> maskedFields) {
        maskedFields.forEach(fieldName -> {
            if (node.has(fieldName)) {
                node.put(fieldName, "***");
            }
        });
    }

    private HttpServletRequest getCurrentHttpRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attrs).getRequest();
        }
        return null;
    }
}
