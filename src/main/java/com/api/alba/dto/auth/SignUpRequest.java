package com.api.alba.dto.auth;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
public class SignUpRequest {
    @Size(min = 4, max = 191, message = "아이디는 4자 이상 191자 이하여야 해요.")
    private String loginId;

    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 해요.")
    private String password;

    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 100, message = "이름은 100자 이하여야 해요.")
    private String name;

    @NotBlank(message = "사용자 유형을 입력해 주세요.")
    @Pattern(regexp = "^(?i)(OWNER|STAFF|PERSONAL)$", message = "사용자 유형이 올바르지 않아요.")
    private String userType;

    @DecimalMin(value = "0.00", inclusive = true, message = "시급은 0 이상이어야 해요.")
    @Digits(integer = 8, fraction = 2, message = "시급 형식이 올바르지 않아요.")
    private BigDecimal hourlyWage;

    @NotEmpty(message = "약관 동의 정보를 입력해 주세요.")
    private List<Long> termsIds;

    @Pattern(regexp = "^(?i)(KAKAO|GOOGLE|APPLE)$", message = "소셜 로그인 제공자가 올바르지 않아요.")
    private String provider;

    @Size(max = 191, message = "소셜 사용자 ID는 191자 이하여야 해요.")
    private String providerUserId;

    @Size(max = 255, message = "소셜 이메일은 255자 이하여야 해요.")
    private String providerEmail;

    @Size(max = 100, message = "소셜 이름은 100자 이하여야 해요.")
    private String providerName;

    public boolean hasSocialAccount() {
        return hasText(provider) || hasText(providerUserId);
    }

    @AssertTrue(message = "소셜 로그인 정보를 올바르게 입력해 주세요.")
    public boolean isSocialAccountValid() {
        return hasText(provider) == hasText(providerUserId);
    }

    @AssertTrue(message = "비밀번호를 입력해 주세요.")
    public boolean isPasswordValid() {
        if (hasSocialAccount()) {
            return !hasText(password) || password.length() >= 8;
        }
        return hasText(password) && password.length() >= 8;
    }

    @AssertTrue(message = "아이디를 입력해 주세요.")
    public boolean isLoginIdValid() {
        if (hasSocialAccount()) {
            return hasText(providerUserId);
        }
        return hasText(loginId);
    }

    @AssertTrue(message = "시급을 입력해 주세요.")
    public boolean isHourlyWageValid() {
        if (!hasText(userType)) {
            return true;
        }
        if ("PERSONAL".equalsIgnoreCase(userType.trim())) {
            return hourlyWage != null;
        }
        return true;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
