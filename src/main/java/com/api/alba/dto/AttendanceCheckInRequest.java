package com.api.alba.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Getter
@Setter
public class AttendanceCheckInRequest {
    @NotNull(message = "workplaceId is required.")
    private Long workplaceId;

    private LocalDate workDate;

    @Size(max = 500, message = "note must be 500 characters or fewer.")
    private String note;
}
