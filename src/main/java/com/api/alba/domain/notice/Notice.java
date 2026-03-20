package com.api.alba.domain.notice;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Notice {
    private Long id;
    private String title;
    private String content;
    private Boolean pinned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
