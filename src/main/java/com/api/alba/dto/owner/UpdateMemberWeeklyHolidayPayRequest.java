package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMemberWeeklyHolidayPayRequest {
    // null이면 매장 설정 상속, true/false면 직원 개별 설정
    private Boolean useWeeklyHolidayPay;
}