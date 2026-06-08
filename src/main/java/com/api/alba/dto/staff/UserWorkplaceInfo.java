package com.api.alba.dto.staff;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserWorkplaceInfo {
    private Long workplaceId;
    private String workplaceName;
    private Boolean isPersonal;
    private Boolean receiveAttendancePush;
    private Integer memberCount;
    private Boolean useQrAttendance;
}
