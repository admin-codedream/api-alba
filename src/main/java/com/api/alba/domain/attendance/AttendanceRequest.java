package com.api.alba.domain.attendance;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AttendanceRequest {
    private Long id;
    private Long attendanceRecordId;
    private Long userId;
    private String type;
    private LocalDateTime requestedCheckInAt;
    private LocalDateTime requestedCheckOutAt;
    private String reason;
    private String status;
    private LocalDateTime createdAt;
}
