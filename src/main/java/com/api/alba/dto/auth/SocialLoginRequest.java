package com.api.alba.dto.auth;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
public class SocialLoginRequest {
    @NotBlank(message = "provider is required.")
    @Pattern(regexp = "^(?i)(KAKAO|GOOGLE|APPLE)$", message = "provider must be one of KAKAO, GOOGLE, APPLE.")
    private String provider;

    @NotBlank(message = "providerUserId is required.")
    @Size(max = 191, message = "providerUserId must be 191 characters or fewer.")
    private String providerUserId;

    @Size(max = 255, message = "providerEmail must be 255 characters or fewer.")
    private String providerEmail;

    @Size(max = 100, message = "providerName must be 100 characters or fewer.")
    private String providerName;
}
