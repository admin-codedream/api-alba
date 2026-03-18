package com.api.alba.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MeResponse {
    private Long id;
    private String loginId;
    private String name;
    private String userType;
    private String status;
}
