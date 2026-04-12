package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class OwnerCreateAttendanceRecordRequest {

    @NotNull(message = "staffUserId is required.")
    private Long staffUserId;

    @NotNull(message = "workDate is required.")
    private LocalDate workDate;

    @NotNull(message = "checkInAt is required.")
    private LocalDateTime checkInAt;

    private LocalDateTime checkOutAt;

    @Size(max = 500, message = "note must be 500 characters or fewer.")
    private String note;
}