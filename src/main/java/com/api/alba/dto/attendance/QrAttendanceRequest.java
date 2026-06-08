package com.api.alba.dto.attendance;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Getter
@Setter
public class QrAttendanceRequest {
    @NotBlank(message = "QR 토큰을 입력해 주세요.")
    private String token;

    @NotNull(message = "출퇴근 타입을 입력해 주세요.")
    @Pattern(regexp = "IN|OUT", message = "출퇴근 타입은 IN 또는 OUT이어야 해요.")
    private String type;
}
