package com.api.alba.domain.notice;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WorkplaceNotice {
    private Long id;
    private Long workplaceId;
    private Long authorId;
    private String title;
    private String content;
    private Boolean pinned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}