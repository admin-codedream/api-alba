package com.api.alba.dto.staff;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class JoinWorkplaceRequest {
    @NotBlank(message = "초대 코드를 입력해 주세요.")
    @Size(max = 20, message = "초대 코드는 20자 이하여야 해요.")
    private String inviteCode;
}
