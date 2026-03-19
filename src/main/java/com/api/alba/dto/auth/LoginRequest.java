package com.api.alba.dto.auth;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "loginId is required.")
    private String loginId;

    @NotBlank(message = "password is required.")
    private String password;
}
