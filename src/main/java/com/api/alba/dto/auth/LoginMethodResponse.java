package com.api.alba.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginMethodResponse {
    private String method; // "PASSWORD" or "OTP"
}