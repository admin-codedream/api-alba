package com.api.alba.dto.auth;

import lombok.Getter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Getter
public class WebOtpConfirmRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String code;
}