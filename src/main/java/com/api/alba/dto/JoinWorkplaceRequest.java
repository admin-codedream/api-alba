package com.api.alba.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class JoinWorkplaceRequest {
    @NotBlank(message = "inviteCode is required.")
    @Size(max = 20, message = "inviteCode must be 20 characters or fewer.")
    private String inviteCode;
}
