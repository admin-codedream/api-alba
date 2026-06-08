package com.api.alba.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QrTokenResponse {
    private String token;
    private String expiresAt;
}
