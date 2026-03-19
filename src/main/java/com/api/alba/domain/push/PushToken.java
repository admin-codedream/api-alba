package com.api.alba.domain.push;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PushToken {
    private Long id;
    private Long userId;
    private String platform;
    private String token;
    private Boolean isActive;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
