package com.api.alba.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MeResponse {
    private Long id;
    private Long workplaceId;
    private String workplaceName;
    private String loginId;
    private String name;
    private String profileInitial;
    private String profileColor;
    private String userType;
    private String status;
}
