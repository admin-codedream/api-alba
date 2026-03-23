package com.api.alba.dto.push;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerPushTokenTarget {
    private String staffName;
    private Long pushTokenId;
    private Long ownerUserId;
    private String token;
}
