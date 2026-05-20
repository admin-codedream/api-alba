package com.api.alba.dto.push;

import lombok.Data;

@Data
public class OwnerPendingRequestTarget {
    private Long workplaceId;
    private String workplaceName;
    private String token;
    private int pendingCount;
}