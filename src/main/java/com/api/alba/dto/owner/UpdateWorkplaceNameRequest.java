package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class UpdateWorkplaceNameRequest {
    @NotBlank(message = "근무지 이름을 입력해 주세요.")
    private String workplaceName;
}