package com.api.alba.dto.auth;

import com.api.alba.component.Masked;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
public class SocialLoginRequest {
    @NotBlank(message = "소셜 로그인 제공자를 입력해 주세요.")
    @Pattern(regexp = "^(?i)(KAKAO|GOOGLE|APPLE)$", message = "소셜 로그인 제공자가 올바르지 않아요.")
    private String provider;

    @Masked
    @NotBlank(message = "소셜 사용자 ID를 입력해 주세요.")
    @Size(max = 191, message = "소셜 사용자 ID는 191자 이하여야 해요.")
    private String providerUserId;

    @Masked
    @Size(max = 255, message = "소셜 이메일은 255자 이하여야 해요.")
    private String providerEmail;

    @Size(max = 100, message = "소셜 이름은 100자 이하여야 해요.")
    private String providerName;
}
