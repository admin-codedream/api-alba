package com.api.alba.dto.auth;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class PasswordResetRequest {
    @NotBlank(message = "아이디를 입력해 주세요.")
    @Size(max = 191, message = "아이디는 191자 이하여야 해요.")
    private String loginId;
}
