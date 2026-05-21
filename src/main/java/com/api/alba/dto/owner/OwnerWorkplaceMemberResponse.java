package com.api.alba.dto.owner;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    @JsonFormat(pattern = "yy.MM.dd")
    private LocalDateTime joinedAt;
    private List<MemberScheduleItemResponse> scheduleDays;
}
