package com.api.alba.domain.auth;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class User {
    private Long id;
    private String loginId;
    private String passwordHash;
    private String name;
    private String profileInitial;
    private String profileColor;
    private String userType;
    private String status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
