package com.api.alba.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Getter
@Setter
public class AttendanceCorrectionRequestCreateRequest {
    private LocalDateTime requestedCheckInAt;
    private LocalDateTime requestedCheckOutAt;

    @NotBlank(message = "reason is required.")
    @Size(max = 1000, message = "reason must be 1000 characters or fewer.")
    private String reason;
}
