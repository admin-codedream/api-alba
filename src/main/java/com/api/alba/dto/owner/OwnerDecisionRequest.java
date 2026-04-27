package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Getter
@Setter
public class OwnerDecisionRequest {
    @NotBlank(message = "처리 상태를 입력해 주세요.")
    @Pattern(regexp = "^(APPROVED|REJECTED)$", message = "처리 상태가 올바르지 않아요.")
    private String status;
}
