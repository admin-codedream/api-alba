package com.api.alba.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class PushTokenDeactivateRequest {
    @NotBlank(message = "token is required.")
    @Size(max = 512, message = "token must be 512 characters or fewer.")
    private String token;
}
