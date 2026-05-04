package com.api.alba.dto.owner;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class MemberScheduleItemResponse {
    private Integer dayOfWeek;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime scheduledCheckInTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime scheduledCheckOutTime;
}