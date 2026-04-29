package com.api.alba.dto.staff;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class StaffWorkplaceSettingResponse {
    private Long workplaceId;
    private String workplaceName;
    private String address;
    private Double latitude;
    private Double longitude;
    private Boolean useLocationRestriction;
    private Boolean receiveAttendancePush;
    private BigDecimal hourlyWage;
    private String salaryCalcUnit;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime defaultCheckInTime;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime defaultCheckOutTime;
    private Integer breakMinutes;
}