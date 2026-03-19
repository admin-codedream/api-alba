package com.api.alba.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Getter
@Setter
public class AttendanceCheckOutRequest {
    @NotNull(message = "workplaceId is required.")
    private Long workplaceId;

    private LocalDate workDate;

    @DecimalMin(value = "-90.0", message = "latitude must be greater than or equal to -90.")
    @DecimalMax(value = "90.0", message = "latitude must be less than or equal to 90.")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "longitude must be greater than or equal to -180.")
    @DecimalMax(value = "180.0", message = "longitude must be less than or equal to 180.")
    private Double longitude;
}
