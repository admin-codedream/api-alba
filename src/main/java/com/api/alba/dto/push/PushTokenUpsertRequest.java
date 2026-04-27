package com.api.alba.dto.push;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
public class PushTokenUpsertRequest {
    @NotBlank(message = "플랫폼을 입력해 주세요.")
    @Pattern(regexp = "^(?i)(IOS|ANDROID|WEB)$", message = "플랫폼이 올바르지 않아요.")
    private String platform;

    @NotBlank(message = "토큰을 입력해 주세요.")
    @Size(max = 512, message = "토큰은 512자 이하여야 해요.")
    private String token;
}
