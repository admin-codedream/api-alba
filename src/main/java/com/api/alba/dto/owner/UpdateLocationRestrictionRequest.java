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
    @NotNull(message = "useLocationRestriction is required.")
    private Boolean useLocationRestriction;

    @Size(max = 255, message = "address must be 255 characters or fewer.")
    private String address;

    @DecimalMin(value = "-90.0", message = "latitude must be greater than or equal to -90.")
    @DecimalMax(value = "90.0", message = "latitude must be less than or equal to 90.")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "longitude must be greater than or equal to -180.")
    @DecimalMax(value = "180.0", message = "longitude must be less than or equal to 180.")
    private Double longitude;
}