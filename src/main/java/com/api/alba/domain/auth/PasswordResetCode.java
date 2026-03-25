package com.api.alba.domain.auth;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PasswordResetCode {
    private Long id;
    private Long userId;
    private String email;
    private String code;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
}
