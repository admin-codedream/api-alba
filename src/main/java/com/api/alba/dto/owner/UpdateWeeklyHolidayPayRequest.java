package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class UpdateWeeklyHolidayPayRequest {
    @NotNull(message = "주휴수당 사용 여부를 입력해 주세요.")
    private Boolean useWeeklyHolidayPay;
}