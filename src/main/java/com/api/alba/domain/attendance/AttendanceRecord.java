package com.api.alba.domain.attendance;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class AttendanceRecord {
    private Long id;
    private Long workplaceId;
    private Long userId;
    private String userName;
    private String profileColor;
    private LocalDate workDate;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private String status;
    private Integer workedMinutes;
    private BigDecimal baseWage;
    private BigDecimal finalWage;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
