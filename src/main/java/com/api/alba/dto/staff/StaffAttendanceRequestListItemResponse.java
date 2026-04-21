package com.api.alba.dto.staff;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class StaffAttendanceRequestListItemResponse {
    private Long requestId;
    private LocalDate workDate;
    private String type;
    private LocalDateTime requestedCheckInAt;
    private LocalDateTime requestedCheckOutAt;
    private String reason;
    private String status;
    private LocalDateTime requestedAt;
}
