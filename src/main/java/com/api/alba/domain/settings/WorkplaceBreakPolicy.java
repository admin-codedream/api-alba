package com.api.alba.domain.settings;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WorkplaceBreakPolicy {
    private Long id;
    private Long workplaceId;
    private String name;
    private String breakType;
    private Integer minWorkMinutes;
    private Integer breakMinutes;
    private Boolean isPaid;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}