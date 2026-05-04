package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class SaveMemberScheduleRequest {

    @Valid
    @NotNull(message = "스케줄 목록을 입력해 주세요.")
    private List<ScheduleItem> schedules;

    @Getter
    @Setter
    public static class ScheduleItem {
        @NotNull(message = "요일을 입력해 주세요.")
        @Min(value = 1, message = "요일은 1(월)~7(일) 사이여야 해요.")
        @Max(value = 7, message = "요일은 1(월)~7(일) 사이여야 해요.")
        private Integer dayOfWeek;

        private LocalTime scheduledCheckInTime;
        private LocalTime scheduledCheckOutTime;
    }
}