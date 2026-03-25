package com.api.alba.dto.owner;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;

@Getter
@Setter
public class UpdateWorkplaceMemberMemoRequest {
    @Size(max = 1000, message = "memo must be 1000 characters or fewer.")
    private String memo;
}
