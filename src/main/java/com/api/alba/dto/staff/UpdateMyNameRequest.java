package com.api.alba.dto.staff;

import lombok.Getter;

import javax.validation.constraints.NotBlank;

@Getter
public class UpdateMyNameRequest {
    @NotBlank(message = "이름을 입력해 주세요.")
    private String name;
}