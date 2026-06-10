package com.api.alba.dto.owner;

import lombok.Getter;

import javax.validation.constraints.NotBlank;

@Getter
public class UpdateMemberRoleRequest {
    @NotBlank(message = "역할을 입력해 주세요.")
    private String role; // STAFF or MANAGER
}