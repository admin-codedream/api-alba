package com.api.alba.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JoinWorkplaceResponse {
    private Long workplaceId;
    private String workplaceName;
    private String role;
    private String joinStatus;
}
