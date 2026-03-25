package com.api.alba.dto.auth;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
public class PasswordResetConfirmRequest {
    @NotBlank(message = "loginId is required.")
    @Size(max = 191, message = "loginId must be 191 characters or fewer.")
    private String loginId;

    @NotBlank(message = "code is required.")
    @Pattern(regexp = "^\\d{6}$", message = "code must be 6 digits.")
    private String code;

    @NotBlank(message = "newPassword is required.")
    @Size(min = 8, max = 100, message = "newPassword must be between 8 and 100 characters.")
    private String newPassword;
}
