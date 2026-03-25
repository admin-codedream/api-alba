package com.api.alba.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class StaffWorkDetailResponse {
    private Long workplaceId;
    private LocalDate workDate;
    private Boolean hasRecord;
    private String attendanceStatus;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private Integer workedMinutes;
    private BigDecimal finalWage;
}
