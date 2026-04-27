package com.api.alba.dto.attendance;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Getter
@Setter
public class AttendanceCheckOutRequest {
    @NotNull(message = "근무지 정보를 입력해 주세요.")
    private Long workplaceId;

    private LocalDate workDate;

    @DecimalMin(value = "-90.0", message = "위도 값이 올바르지 않아요.")
    @DecimalMax(value = "90.0", message = "위도 값이 올바르지 않아요.")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "경도 값이 올바르지 않아요.")
    @DecimalMax(value = "180.0", message = "경도 값이 올바르지 않아요.")
    private Double longitude;
}
