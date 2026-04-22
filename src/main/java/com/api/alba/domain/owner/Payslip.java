package com.api.alba.domain.owner;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class Payslip {
    private Long id;
    private Long workplaceId;
    private Long userId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal hourlyWage;

    // USERS 테이블 조인으로 채워지는 필드
    private String userName;
    private String profileColor;
    private int workedDays;
    private int workedMinutes;
    private BigDecimal baseWage;
    private BigDecimal bonusAmount;
    private String bonusNote;
    private BigDecimal deductionAmount;
    private String deductionNote;
    private BigDecimal totalWage;
    private String dailySnapshot;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 직원 조회 시 JOIN으로 채워지는 필드 (컬럼 없음)
    private String workplaceName;
}
