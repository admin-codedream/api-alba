package com.api.alba.dto.staff;

import lombok.Getter;

import javax.validation.constraints.Min;

@Getter
public class UpdateMyBreakMinutesRequest {
    @Min(0)
    private Integer breakMinutes;
}