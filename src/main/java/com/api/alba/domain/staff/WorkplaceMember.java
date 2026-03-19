package com.api.alba.domain.staff;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WorkplaceMember {
    private Long id;
    private Long workplaceId;
    private Long userId;
    private String role;
    private BigDecimal hourlyWage;
    private String status;
}
