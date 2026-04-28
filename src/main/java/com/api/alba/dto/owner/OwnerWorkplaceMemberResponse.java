package com.api.alba.dto.owner;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class OwnerWorkplaceMemberResponse {
    private Long memberId;
    private Long userId;
    private String name;
    private String profileColor;
    private String role;
    private BigDecimal hourlyWage;
    private String status;
    private String memo;
    private Integer breakMinutes;
}
