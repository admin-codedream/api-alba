package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Getter
@Setter
public class CreateWorkplaceRequest {
    @NotBlank(message = "근무지 이름을 입력해 주세요.")
    @Size(max = 150, message = "근무지 이름은 150자 이하여야 해요.")
    private String name;

    @Size(max = 255, message = "주소는 255자 이하여야 해요.")
    private String address;

    @DecimalMin(value = "-90.0", message = "위도 값이 올바르지 않아요.")
    @DecimalMax(value = "90.0", message = "위도 값이 올바르지 않아요.")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "경도 값이 올바르지 않아요.")
    @DecimalMax(value = "180.0", message = "경도 값이 올바르지 않아요.")
    private Double longitude;

    @Min(value = 10, message = "허용 반경은 10 이상이어야 해요.")
    @Max(value = 5000, message = "허용 반경은 5000 이하여야 해요.")
    private Integer allowedRadiusMeters;

    private Boolean useLocationRestriction;

    @DecimalMin(value = "0.00", inclusive = true, message = "시급은 0 이상이어야 해요.")
    @Digits(integer = 8, fraction = 2, message = "시급 형식이 올바르지 않아요.")
    private BigDecimal hourlyWage;
}
