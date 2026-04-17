package com.api.alba.dto.push;

import lombok.Data;

@Data
public class StaffReminderTarget {
    private Long workplaceId;
    private String workplaceName;
    private Long userId;
    private String token;
}
