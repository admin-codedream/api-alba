package com.api.alba.dto.auth;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
public class PasswordResetConfirmRequest {
    @NotBlank(message = "아이디를 입력해 주세요.")
    @Size(max = 191, message = "아이디는 191자 이하여야 해요.")
    private String loginId;

    @NotBlank(message = "인증 코드를 입력해 주세요.")
    @Pattern(regexp = "^\\d{6}$", message = "인증 코드는 6자리 숫자여야 해요.")
    private String code;

    @NotBlank(message = "새 비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 100, message = "새 비밀번호는 8자 이상 100자 이하여야 해요.")
    private String newPassword;
}
