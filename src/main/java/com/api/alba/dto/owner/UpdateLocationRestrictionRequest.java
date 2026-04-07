package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class UpdateLocationRestrictionRequest {
    @NotNull(message = "useLocationRestriction is required.")
    private Boolean useLocationRestriction;
}