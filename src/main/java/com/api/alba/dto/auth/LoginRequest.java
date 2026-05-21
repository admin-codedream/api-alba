package com.api.alba.dto.auth;

import com.api.alba.component.Masked;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "아이디를 입력해 주세요.")
    private String loginId;

    @Masked
    @NotBlank(message = "비밀번호를 입력해 주세요.")
    private String password;
}
