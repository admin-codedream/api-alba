package com.api.alba.dto.owner;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class UpdateDefaultWorkTimeRequest {
    @JsonFormat(pattern = "HH:mm")
    private LocalTime defaultCheckInTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime defaultCheckOutTime;
}
