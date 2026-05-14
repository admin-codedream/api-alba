package com.api.alba.dto.contract;

import lombok.Getter;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
public class LaborContractCreateRequest {

    @NotNull(message = "직원을 선택해 주세요.")
    private Long employeeUserId;

    @NotNull(message = "계약 시작일을 입력해 주세요.")
    private LocalDate contractStartDate;

    private LocalDate contractEndDate;

    @Size(max = 500, message = "담당 업무는 500자 이하여야 해요.")
    private String jobDescription;

    @NotNull(message = "근무 요일을 입력해 주세요.")
    @Min(value = 1, message = "근무 요일을 선택해 주세요.")
    @Max(value = 127, message = "근무 요일 값이 올바르지 않아요.")
    private Integer workDays;

    @NotNull(message = "근무 시작 시간을 입력해 주세요.")
    private LocalTime workStartTime;

    @NotNull(message = "근무 종료 시간을 입력해 주세요.")
    private LocalTime workEndTime;

    @NotNull(message = "휴게 시간을 입력해 주세요.")
    @Min(value = 0, message = "휴게 시간은 0분 이상이어야 해요.")
    private Integer breakMinutes;

    @NotNull(message = "급여 유형을 입력해 주세요.")
    private String wageType; // HOURLY or MONTHLY

    @DecimalMin(value = "0.00", inclusive = true, message = "시급은 0 이상이어야 해요.")
    @Digits(integer = 8, fraction = 2, message = "시급 형식이 올바르지 않아요.")
    private BigDecimal hourlyWage;

    @DecimalMin(value = "0.00", inclusive = true, message = "월급은 0 이상이어야 해요.")
    @Digits(integer = 10, fraction = 2, message = "월급 형식이 올바르지 않아요.")
    private BigDecimal monthlyWage;

    @NotNull(message = "급여 지급일을 입력해 주세요.")
    @Min(value = 1, message = "급여 지급일은 1일 이상이어야 해요.")
    @Max(value = 31, message = "급여 지급일은 31일 이하여야 해요.")
    private Integer paymentDay;

    private Boolean useNationalPension;
    private Boolean useHealthInsurance;
    private Boolean useEmpInsurance;
}