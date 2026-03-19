package com.api.alba.dto.auth;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
public class SignUpRequest {
    @NotBlank(message = "loginId is required.")
    @Size(min = 4, max = 50, message = "loginId must be between 4 and 50 characters.")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "loginId can contain letters, numbers, dot, underscore, and hyphen only.")
    private String loginId;

    @NotBlank(message = "password is required.")
    @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters.")
    private String password;

    @NotBlank(message = "name is required.")
    @Size(max = 100, message = "name must be 100 characters or fewer.")
    private String name;

    @NotBlank(message = "userType is required.")
    @Pattern(regexp = "^(?i)(OWNER|STAFF)$", message = "userType must be OWNER or STAFF.")
    private String userType;
}
