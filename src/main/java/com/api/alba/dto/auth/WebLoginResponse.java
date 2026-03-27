package com.api.alba.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class WebLoginResponse {
    private String accessToken;
    private String tokenType;
    private long expiresInSeconds;
    private String userType;
    private List<WorkplaceItem> workplaces;

    @Getter
    @AllArgsConstructor
    public static class WorkplaceItem {
        private Long workplaceId;
        private String workplaceName;
        private Boolean isPersonal;
    }
}
