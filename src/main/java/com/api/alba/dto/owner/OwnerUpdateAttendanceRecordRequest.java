package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Getter
@Setter
public class OwnerUpdateAttendanceRecordRequest {

    @NotNull(message = "출근 시간을 입력해 주세요.")
    private LocalDateTime checkInAt;

    private LocalDateTime checkOutAt;

    @Size(max = 500, message = "메모는 500자 이하여야 해요.")
    private String note;
}