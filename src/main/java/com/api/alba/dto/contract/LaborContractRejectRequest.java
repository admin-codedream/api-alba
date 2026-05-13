package com.api.alba.dto.contract;

import lombok.Getter;

import javax.validation.constraints.Size;

@Getter
public class LaborContractRejectRequest {

    @Size(max = 500, message = "거절 사유는 500자 이하여야 해요.")
    private String reason;
}