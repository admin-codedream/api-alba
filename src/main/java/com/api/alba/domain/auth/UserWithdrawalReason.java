package com.api.alba.domain.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserWithdrawalReason {
    private Long id;
    private Long userId;
    private String reasonType;
    private String customReason;
}