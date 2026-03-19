package com.api.alba.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AttendanceRequestCreatedResponse {
    private Long requestId;
    private String status;
}
