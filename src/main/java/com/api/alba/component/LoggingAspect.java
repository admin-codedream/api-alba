package com.api.alba.component;


import javax.servlet.http.HttpServletRequest;
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

@Slf4j
@Aspect
@Component
public class LoggingAspect {

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
                    .append(paramValues[i]).append(", ");
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

    private HttpServletRequest getCurrentHttpRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attrs).getRequest();
        }
        return null;
    }
}
