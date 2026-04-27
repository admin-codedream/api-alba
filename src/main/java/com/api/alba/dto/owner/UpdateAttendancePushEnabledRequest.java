package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class UpdateAttendancePushEnabledRequest {
    @NotNull(message = "알림 설정 여부를 입력해 주세요.")
    private Boolean enabled;
}