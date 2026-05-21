package com.api.alba.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 로그 출력 시 해당 필드 값을 "***"로 마스킹합니다.
 * 비밀번호, 이메일 등 민감한 개인정보 필드에 사용합니다.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Masked {
}