package com.api.alba.domain.terms;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Terms {
    private Long id;
    private String termsType;
    private String version;
    private String title;
    private String content;
    private Boolean isRequired;
    private Boolean isActive;
    private LocalDateTime effectiveAt;
    private LocalDateTime createdAt;
}