package com.api.alba.dto.owner;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayslipDailyItemResponse {
    private LocalDate workDate;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private int workedMinutes;
    private BigDecimal finalWage;
    private String status;
    private String wageType;
    private BigDecimal monthlyWage;
    private BigDecimal dailyWage;
}
