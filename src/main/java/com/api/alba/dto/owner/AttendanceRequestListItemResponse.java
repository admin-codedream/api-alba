package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class AttendanceRequestListItemResponse {
    private Long requestId;
    private Long attendanceRecordId;
    private Long userId;
    private String userName;
    private LocalDate workDate;
    private String type;
    private LocalDateTime requestedCheckInAt;
    private LocalDateTime requestedCheckOutAt;
    private String reason;
    private String status;
    private LocalDateTime requestedAt;
}
