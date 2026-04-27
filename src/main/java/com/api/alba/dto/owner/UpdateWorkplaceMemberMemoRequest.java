package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;

@Getter
@Setter
public class UpdateWorkplaceMemberMemoRequest {
    @Size(max = 1000, message = "메모는 1000자 이하여야 해요.")
    private String memo;
}
