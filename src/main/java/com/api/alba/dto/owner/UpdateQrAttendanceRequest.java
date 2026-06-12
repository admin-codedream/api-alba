package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class UpdateQrAttendanceRequest {
    @NotNull(message = "QR 출퇴근 사용 여부를 입력해 주세요.")
    private Boolean useQrAttendance;
    private Boolean qrNoTimeLimit;
    private String qrPin;
}
