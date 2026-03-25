package com.api.alba.dto.auth;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class PasswordResetRequest {
    @NotBlank(message = "loginId is required.")
    @Size(max = 191, message = "loginId must be 191 characters or fewer.")
    private String loginId;
}
