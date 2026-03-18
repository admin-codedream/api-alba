package com.api.alba.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Getter
@Setter
public class AttendanceCheckOutRequest {
    @NotNull(message = "workplaceId is required.")
    private Long workplaceId;

    private LocalDate workDate;
}
