package com.api.alba.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Workplace {
    private Long id;
    private Long ownerId;
    private String name;
    private String address;
    private String inviteCode;
    private LocalDateTime createdAt;
}
