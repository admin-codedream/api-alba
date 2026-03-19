package com.api.alba.domain.owner;

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
    private Double latitude;
    private Double longitude;
    private Integer allowedRadiusMeters;
    private LocalDateTime createdAt;
}
