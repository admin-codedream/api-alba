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
    @NotBlank(message = "name is required.")
    @Size(max = 150, message = "name must be 150 characters or fewer.")
    private String name;

    @Size(max = 255, message = "address must be 255 characters or fewer.")
    private String address;

    @DecimalMin(value = "-90.0", message = "latitude must be greater than or equal to -90.")
    @DecimalMax(value = "90.0", message = "latitude must be less than or equal to 90.")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "longitude must be greater than or equal to -180.")
    @DecimalMax(value = "180.0", message = "longitude must be less than or equal to 180.")
    private Double longitude;

    @Min(value = 10, message = "allowedRadiusMeters must be 10 or greater.")
    @Max(value = 5000, message = "allowedRadiusMeters must be 5000 or fewer.")
    private Integer allowedRadiusMeters;

    @DecimalMin(value = "0.00", inclusive = true, message = "hourlyWage must be 0 or greater.")
    @Digits(integer = 8, fraction = 2, message = "hourlyWage must have up to 8 integer digits and 2 decimal places.")
    private BigDecimal hourlyWage;
}
