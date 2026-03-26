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
    @Size(min = 4, max = 191, message = "loginId must be between 4 and 191 characters.")
    private String loginId;

    @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters.")
    private String password;

    @NotBlank(message = "name is required.")
    @Size(max = 100, message = "name must be 100 characters or fewer.")
    private String name;

    @NotBlank(message = "userType is required.")
    @Pattern(regexp = "^(?i)(OWNER|STAFF|PERSONAL)$", message = "userType must be OWNER, STAFF, or PERSONAL.")
    private String userType;

    @DecimalMin(value = "0.00", inclusive = true, message = "hourlyWage must be 0 or greater.")
    @Digits(integer = 8, fraction = 2, message = "hourlyWage must have up to 8 integer digits and 2 decimal places.")
    private BigDecimal hourlyWage;

    @NotEmpty(message = "termsIds is required.")
    private List<Long> termsIds;

    @Pattern(regexp = "^(?i)(KAKAO|GOOGLE|APPLE)$", message = "provider must be one of KAKAO, GOOGLE, APPLE.")
    private String provider;

    @Size(max = 191, message = "providerUserId must be 191 characters or fewer.")
    private String providerUserId;

    @Size(max = 255, message = "providerEmail must be 255 characters or fewer.")
    private String providerEmail;

    @Size(max = 100, message = "providerName must be 100 characters or fewer.")
    private String providerName;

    public boolean hasSocialAccount() {
        return hasText(provider) || hasText(providerUserId);
    }

    @AssertTrue(message = "provider and providerUserId must be provided together.")
    public boolean isSocialAccountValid() {
        return hasText(provider) == hasText(providerUserId);
    }

    @AssertTrue(message = "password is required for non-social signup.")
    public boolean isPasswordValid() {
        if (hasSocialAccount()) {
            return !hasText(password) || password.length() >= 8;
        }
        return hasText(password) && password.length() >= 8;
    }

    @AssertTrue(message = "loginId is required for non-social signup.")
    public boolean isLoginIdValid() {
        if (hasSocialAccount()) {
            return hasText(providerUserId);
        }
        return hasText(loginId);
    }

    @AssertTrue(message = "hourlyWage is required for PERSONAL signup.")
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
