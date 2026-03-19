package com.api.alba.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
public class PushTokenUpsertRequest {
    @NotBlank(message = "platform is required.")
    @Pattern(regexp = "^(?i)(IOS|ANDROID|WEB)$", message = "platform must be IOS, ANDROID, or WEB.")
    private String platform;

    @NotBlank(message = "token is required.")
    @Size(max = 512, message = "token must be 512 characters or fewer.")
    private String token;
}
