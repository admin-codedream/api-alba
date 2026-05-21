package com.api.alba.dto.auth;

import com.api.alba.component.Masked;
import lombok.Getter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Getter
public class WebLoginMethodRequest {
    @Masked
    @NotBlank
    @Email
    private String email;
}