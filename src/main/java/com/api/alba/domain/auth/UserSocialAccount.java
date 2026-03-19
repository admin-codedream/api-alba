package com.api.alba.domain.auth;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserSocialAccount {
    private Long id;
    private Long userId;
    private String provider;
    private String providerUserId;
    private String providerEmail;
    private String providerName;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime connectedAt;
    private LocalDateTime lastLoginAt;
}
