package com.api.alba.dto.owner;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AttendancePushSettingResponse {
    private Long workplaceId;
    private Boolean enabled;
}
