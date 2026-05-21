package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class OwnerWorkplaceMemberResponse {
    private Long memberId;
    private Long userId;
    private String name;
    private String profileColor;
    private String role;
    private String wageType;
    private BigDecimal hourlyWage;
    private BigDecimal monthlyWage;
    private String status;
    private String memo;
    private Integer breakMinutes;
    private List<MemberScheduleItemResponse> scheduleDays;
}
