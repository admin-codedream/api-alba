package com.api.alba.domain.contract;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
public class LaborContract {
    private Long id;
    private Long workplaceId;
    private Long employeeUserId;
    private String status;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private String workplaceName;
    private String workplaceAddress;
    private String ownerName;
    private String employeeName;
    private String jobDescription;
    private Integer workDays;
    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private Integer breakMinutes;
    private String wageType;
    private BigDecimal hourlyWage;
    private BigDecimal monthlyWage;
    private Integer paymentDay;
    private Boolean useNationalPension;
    private Boolean useHealthInsurance;
    private Boolean useEmpInsurance;
    private LocalDateTime ownerSignedAt;
    private LocalDateTime employeeSignedAt;
    private LocalDateTime sentAt;
    private String rejectedReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}