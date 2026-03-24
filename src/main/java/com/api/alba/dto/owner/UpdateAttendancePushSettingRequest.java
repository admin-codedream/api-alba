package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class UpdateAttendancePushSettingRequest {
    @NotNull(message = "enabled is required.")
    private Boolean enabled;
}
