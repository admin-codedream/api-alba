package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class UpdateWorkplaceNameRequest {
    @NotBlank(message = "workplaceName is required.")
    private String workplaceName;
}