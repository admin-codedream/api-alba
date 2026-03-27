package com.api.alba.dto.attendance;

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

    @NotBlank(message = "사유는 필수입니다.")
    @Size(max = 1000, message = "reason must be 1000 characters or fewer.")
    private String reason;
}
