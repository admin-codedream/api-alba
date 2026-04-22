package com.api.alba.dto.owner;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class IssuedPayslipListItemResponse {
    private Long id;
    private Long memberId;
    private String staffName;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalWorkDays;
    private int totalWorkedMinutes;
    private BigDecimal totalWage;
    private String status;
    private LocalDateTime createdAt;
}
