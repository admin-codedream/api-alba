package com.api.alba.dto.owner;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayslipRecordItem {
    private LocalDate workDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime checkInAt;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime checkOutAt;

    private int workedMinutes;
    private BigDecimal dailyWage;
}
