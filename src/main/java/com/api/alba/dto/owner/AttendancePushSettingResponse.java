package com.api.alba.dto.owner;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class AttendancePushSettingResponse {
    private Long workplaceId;
    private Boolean enabled;
    private BigDecimal hourlyWage;
}
