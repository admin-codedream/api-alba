package com.api.alba.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Getter
@Setter
public class OwnerDecisionRequest {
    @NotBlank(message = "status is required.")
    @Pattern(regexp = "^(APPROVED|REJECTED)$", message = "status must be APPROVED or REJECTED.")
    private String status;
}
