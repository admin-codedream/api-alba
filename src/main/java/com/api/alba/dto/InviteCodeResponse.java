package com.api.alba.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InviteCodeResponse {
    private Long workplaceId;
    private String inviteCode;
}
