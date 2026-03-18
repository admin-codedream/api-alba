package com.api.alba.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class CreateWorkplaceRequest {
    @NotBlank(message = "name is required.")
    @Size(max = 150, message = "name must be 150 characters or fewer.")
    private String name;

    @Size(max = 255, message = "address must be 255 characters or fewer.")
    private String address;
}
