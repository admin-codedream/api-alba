package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class UpdateLocationRestrictionRequest {
    @NotNull(message = "위치 제한 사용 여부를 입력해 주세요.")
    private Boolean useLocationRestriction;

    @Size(max = 255, message = "주소는 255자 이하여야 해요.")
    private String address;

    @DecimalMin(value = "-90.0", message = "위도 값이 올바르지 않아요.")
    @DecimalMax(value = "90.0", message = "위도 값이 올바르지 않아요.")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "경도 값이 올바르지 않아요.")
    @DecimalMax(value = "180.0", message = "경도 값이 올바르지 않아요.")
    private Double longitude;
}