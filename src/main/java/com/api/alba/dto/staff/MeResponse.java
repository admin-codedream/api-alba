package com.api.alba.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MeResponse {
    private Long id;
    private List<UserWorkplaceInfo> workplaces;
    private String loginId;
    private String name;
    private String profileInitial;
    private String profileColor;
    private String userType;
    private String status;
}
