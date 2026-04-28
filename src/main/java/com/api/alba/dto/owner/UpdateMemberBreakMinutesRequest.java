package com.api.alba.dto.owner;

import lombok.Getter;

import javax.validation.constraints.Min;

@Getter
public class UpdateMemberBreakMinutesRequest {
    @Min(0)
    private Integer breakMinutes;
}