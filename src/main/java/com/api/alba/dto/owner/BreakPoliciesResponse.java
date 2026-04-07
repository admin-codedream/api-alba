package com.api.alba.dto.owner;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class BreakPoliciesResponse {
    private Boolean useBreakPolicy;
    private List<PolicyItem> policies;

    @Getter
    @AllArgsConstructor
    public static class PolicyItem {
        private Long id;
        private String name;
        private String breakType;
        private Integer minWorkMinutes;
        private Integer breakMinutes;
        private Boolean isPaid;
        private Boolean isActive;
    }
}