package com.api.alba.domain.staff;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class WorkplaceMemberSchedule {
    private Long id;
    private Long workplaceId;
    private Long userId;
    private Integer dayOfWeek;
    private LocalTime scheduledCheckInTime;
    private LocalTime scheduledCheckOutTime;
}