package com.api.alba.dto.attendance;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class AttendanceNewRecordRequestCreateRequest {
    @NotNull(message = "근무일은 필수입니다.")
    private LocalDate workDate;

    private LocalDateTime requestedCheckInAt;
    private LocalDateTime requestedCheckOutAt;

    @NotBlank(message = "사유는 필수입니다.")
    @Size(max = 1000, message = "reason must be 1000 characters or fewer.")
    private String reason;
}
