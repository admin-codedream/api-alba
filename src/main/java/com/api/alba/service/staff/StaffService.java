package com.api.alba.service.staff;

import com.api.alba.component.WageCalculationHelper;
import com.api.alba.domain.attendance.AttendanceRecord;
import com.api.alba.domain.attendance.AttendanceRequest;
import com.api.alba.domain.owner.Workplace;
import com.api.alba.domain.settings.WorkplaceBreakPolicy;
import com.api.alba.domain.settings.WorkplaceSetting;
import com.api.alba.domain.staff.WorkplaceMember;
import com.api.alba.dto.attendance.AttendanceNewRecordRequestCreateRequest;
import com.api.alba.dto.attendance.AttendanceRequestCreatedResponse;
import com.api.alba.domain.owner.Payslip;
import com.api.alba.domain.owner.PayslipDeduction;
import com.api.alba.dto.owner.PayslipDeductionItemResponse;
import com.api.alba.dto.owner.PayslipRecordItem;
import com.api.alba.dto.staff.StaffAttendanceRequestListItemResponse;
import com.api.alba.dto.staff.JoinWorkplaceRequest;
import com.api.alba.dto.staff.JoinWorkplaceResponse;
import com.api.alba.dto.staff.MyAggregateSummary;
import com.api.alba.dto.staff.StaffHomeTodayResponse;
import com.api.alba.dto.staff.StaffMonthlyCalendarItemResponse;
import com.api.alba.dto.staff.StaffPayslipDetailResponse;
import com.api.alba.dto.staff.StaffPayslipListItemResponse;
import com.api.alba.dto.staff.StaffTodaySummaryResponse;
import com.api.alba.dto.staff.StaffWorkDetailResponse;
import com.api.alba.dto.staff.StaffWorkplaceSettingResponse;
import com.api.alba.mapper.owner.PayslipDeductionMapper;
import com.api.alba.mapper.owner.PayslipMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.api.alba.exception.ApiException;
import com.api.alba.mapper.attendance.AttendanceRecordMapper;
import com.api.alba.mapper.attendance.AttendanceRequestMapper;
import com.api.alba.mapper.owner.WorkplaceMapper;
import com.api.alba.mapper.settings.WorkplaceBreakPolicyMapper;
import com.api.alba.mapper.settings.WorkplaceSettingMapper;
import com.api.alba.mapper.push.PushTokenMapper;
import com.api.alba.mapper.auth.UserMapper;
import com.api.alba.mapper.staff.WorkplaceMemberMapper;
import com.api.alba.domain.staff.WorkplaceMemberSchedule;
import com.api.alba.mapper.staff.WorkplaceMemberScheduleMapper;
import com.api.alba.dto.owner.MemberScheduleItemResponse;
import com.api.alba.dto.push.OwnerPushTokenTarget;
import com.api.alba.firebase.FcmDto;
import com.api.alba.firebase.FcmService;
import com.api.alba.firebase.ProjectId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.api.alba.exception.ExceptionMessages.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {
    private final UserMapper userMapper;
    private final WorkplaceMapper workplaceMapper;
    private final WorkplaceMemberMapper workplaceMemberMapper;
    private final WorkplaceSettingMapper workplaceSettingMapper;
    private final WorkplaceBreakPolicyMapper workplaceBreakPolicyMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final AttendanceRequestMapper attendanceRequestMapper;
    private final WageCalculationHelper wageCalculationHelper;
    private final PayslipMapper payslipMapper;
    private final PayslipDeductionMapper payslipDeductionMapper;
    private final ObjectMapper objectMapper;
    private final PushTokenMapper pushTokenMapper;
    private final FcmService fcmService;
    private final WorkplaceMemberScheduleMapper workplaceMemberScheduleMapper;

    @Transactional
    public JoinWorkplaceResponse joinWorkplaceByInviteCode(Long userId, JoinWorkplaceRequest request) {
        String inviteCode = request.getInviteCode().trim().toUpperCase(Locale.ROOT);
        Workplace workplace = workplaceMapper.findByInviteCode(inviteCode);
        if (workplace == null) {
            throw new ApiException(INVALID_INVITE_CODE);
        }

        WorkplaceMember activeMember = workplaceMemberMapper.findActiveMember(workplace.getId(), userId);
        if (activeMember != null) {
            return new JoinWorkplaceResponse(workplace.getId(), workplace.getName(), activeMember.getRole(), "ALREADY_JOINED");
        }

        WorkplaceMember existingMember = workplaceMemberMapper.findMember(workplace.getId(), userId);
        if (existingMember == null) {
            WorkplaceMember newMember = new WorkplaceMember();
            newMember.setWorkplaceId(workplace.getId());
            newMember.setUserId(userId);
            newMember.setRole("STAFF");
            newMember.setWageType("HOURLY");
            newMember.setStatus("ACTIVE");
            newMember.setHourlyWage(null);
            newMember.setReceiveAttendancePush(true);
            workplaceMemberMapper.insert(newMember);
        } else {
            workplaceMemberMapper.updateStatus(existingMember.getId(), "ACTIVE");
        }

        WorkplaceMember joinedMember = workplaceMemberMapper.findActiveMember(workplace.getId(), userId);
        if (joinedMember == null) {
            throw new ApiException(FAILED_TO_JOIN_WORKPLACE);
        }
        return new JoinWorkplaceResponse(workplace.getId(), workplace.getName(), joinedMember.getRole(), "JOINED");
    }

    public StaffHomeTodayResponse getHomeToday(Long userId, Long workplaceId) {
        WorkplaceMember member = ensureActiveMember(workplaceId, userId);
        LocalDate today = LocalDate.now();
        AttendanceRecord record = attendanceRecordMapper.findByWorkplaceUserAndDate(workplaceId, userId, today);

        if (record == null) {
            AttendanceRecord yesterday = attendanceRecordMapper.findByWorkplaceUserAndDate(workplaceId, userId, today.minusDays(1));
            if (yesterday != null && yesterday.getCheckOutAt() == null) {
                record = yesterday;
            }
        }

        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        List<WorkplaceBreakPolicy> breakPolicies = resolveBreakPolicies(workplaceId, setting);
        BigDecimal hourlyWage = resolveHourlyWage(member, setting);
        String wageType = resolveWageType(member);
        BigDecimal monthlyWage = resolveMonthlyWage(member);
        if (record == null) {
            return new StaffHomeTodayResponse(
                    workplaceId,
                    today,
                    "BEFORE_CHECK_IN",
                    null,
                    null,
                    0,
                    BigDecimal.ZERO,
                    hourlyWage,
                    wageType,
                    monthlyWage
            );
        }

        int workedMinutes = safeMinutes(record.getWorkedMinutes());
        BigDecimal expectedWage = safeWage(record.getFinalWage());
        if (record.getCheckInAt() != null && record.getCheckOutAt() == null) {
            int grossWorkedMinutes = calculateWorkedMinutes(record.getCheckInAt(), LocalDateTime.now());
            workedMinutes = wageCalculationHelper.calculatePayableWorkedMinutes(grossWorkedMinutes, setting, breakPolicies, member.getBreakMinutes());
            expectedWage = wageCalculationHelper.calculateWage(hourlyWage, workedMinutes);
        }

        return new StaffHomeTodayResponse(
                workplaceId,
                today,
                record.getStatus(),
                record.getCheckInAt(),
                record.getCheckOutAt(),
                workedMinutes,
                expectedWage,
                hourlyWage,
                wageType,
                monthlyWage
        );
    }

    public StaffTodaySummaryResponse getTodaySummary(Long userId, Long workplaceId) {
        WorkplaceMember member = ensureActiveMember(workplaceId, userId);
        LocalDate today = LocalDate.now();
        AttendanceRecord record = attendanceRecordMapper.findByWorkplaceUserAndDate(workplaceId, userId, today);

        if (record == null) {
            AttendanceRecord yesterday = attendanceRecordMapper.findByWorkplaceUserAndDate(workplaceId, userId, today.minusDays(1));
            if (yesterday != null && yesterday.getCheckOutAt() == null) {
                record = yesterday;
            }
        }

        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        List<WorkplaceBreakPolicy> breakPolicies = resolveBreakPolicies(workplaceId, setting);
        BigDecimal hourlyWage = resolveHourlyWage(member, setting);
        int todayWorkedMinutes = 0;
        BigDecimal todayExpectedWage = BigDecimal.ZERO;
        if (record != null) {
            todayWorkedMinutes = safeMinutes(record.getWorkedMinutes());
            todayExpectedWage = safeWage(record.getFinalWage());

            if (record.getCheckInAt() != null && record.getCheckOutAt() == null) {
                int grossWorkedMinutes = calculateWorkedMinutes(record.getCheckInAt(), LocalDateTime.now());
                todayWorkedMinutes = wageCalculationHelper.calculatePayableWorkedMinutes(
                        grossWorkedMinutes,
                        setting,
                        breakPolicies,
                        member.getBreakMinutes()
                );
                todayExpectedWage = wageCalculationHelper.calculateWage(hourlyWage, todayWorkedMinutes);
            }
        }

        MyAggregateSummary aggregate = attendanceRecordMapper.findMyAggregateSummary(workplaceId, userId);
        int cumulativeWorkedMinutes = aggregate == null ? 0 : safeMinutes(aggregate.getTotalWorkedMinutes());
        BigDecimal cumulativeExpectedWage = aggregate == null ? BigDecimal.ZERO : safeWage(aggregate.getTotalExpectedWage());

        return new StaffTodaySummaryResponse(
                workplaceId,
                today,
                todayWorkedMinutes,
                todayExpectedWage,
                cumulativeWorkedMinutes,
                cumulativeExpectedWage,
                resolveWageType(member),
                resolveMonthlyWage(member)
        );
    }

    public List<StaffMonthlyCalendarItemResponse> getMonthlyCalendar(Long userId, Long workplaceId, String yearMonth) {
        ensureActiveMember(workplaceId, userId);
        YearMonth targetMonth = parseYearMonth(yearMonth);
        return attendanceRecordMapper.findMonthlyCalendarByPeriod(
                workplaceId,
                userId,
                targetMonth.atDay(1),
                targetMonth.atEndOfMonth()
        );
    }

    public StaffWorkDetailResponse getWorkDetail(Long userId, Long workplaceId, LocalDate workDate) {
        ensureActiveMember(workplaceId, userId);
        AttendanceRecord record = attendanceRecordMapper.findByWorkplaceUserAndDate(workplaceId, userId, workDate);
        if (record == null) {
            return new StaffWorkDetailResponse(
                    workplaceId,
                    workDate,
                    false,
                    null,
                    null,
                    null,
                    0,
                    BigDecimal.ZERO
            );
        }

        return new StaffWorkDetailResponse(
                workplaceId,
                workDate,
                true,
                record.getStatus(),
                record.getCheckInAt(),
                record.getCheckOutAt(),
                safeMinutes(record.getWorkedMinutes()),
                safeWage(record.getFinalWage())
        );
    }

    public List<StaffAttendanceRequestListItemResponse> getMyAttendanceRequests(Long userId, Long workplaceId) {
        ensureActiveMember(workplaceId, userId);
        return attendanceRequestMapper.findByWorkplaceIdAndUserId(workplaceId, userId);
    }

    @Transactional
    public AttendanceRequestCreatedResponse submitNewRecordRequest(
            Long userId,
            Long workplaceId,
            AttendanceNewRecordRequestCreateRequest request
    ) {
        ensureActiveMember(workplaceId, userId);

        LocalDateTime checkIn = request.getRequestedCheckInAt();
        LocalDateTime checkOut = request.getRequestedCheckOutAt();
        if (checkIn == null && checkOut == null) {
            throw new ApiException(AT_LEAST_ONE_REQUESTED_CHECK_IN_OR_OUT_REQUIRED);
        }
        if (checkIn != null && checkOut != null && checkOut.isBefore(checkIn)) {
            throw new ApiException(REQUESTED_CHECK_OUT_MUST_BE_LATER_THAN_CHECK_IN);
        }

        AttendanceRecord record = attendanceRecordMapper.findByWorkplaceUserAndDate(workplaceId, userId, request.getWorkDate());
        boolean isNewRecord = record == null;
        if (isNewRecord) {
            record = new AttendanceRecord();
            record.setWorkplaceId(workplaceId);
            record.setUserId(userId);
            record.setWorkDate(request.getWorkDate());
            record.setStatus("WORKING");
            record.setWorkedMinutes(0);
            record.setBaseWage(BigDecimal.ZERO);
            record.setFinalWage(BigDecimal.ZERO);
            attendanceRecordMapper.insert(record);
        }

        if (attendanceRequestMapper.countPendingByRecordAndUser(record.getId(), userId) > 0) {
            throw new ApiException(PENDING_CORRECTION_REQUEST_EXISTS);
        }

        String type = resolveNewRequestType(checkIn, checkOut, isNewRecord);

        AttendanceRequest attendanceRequest = new AttendanceRequest();
        attendanceRequest.setAttendanceRecordId(record.getId());
        attendanceRequest.setUserId(userId);
        attendanceRequest.setType(type);
        attendanceRequest.setRequestedCheckInAt(checkIn);
        attendanceRequest.setRequestedCheckOutAt(checkOut);
        attendanceRequest.setReason(request.getReason());
        attendanceRequest.setStatus("PENDING");
        attendanceRequestMapper.insert(attendanceRequest);

        sendCorrectionRequestPushSafely(workplaceId, userId);

        return new AttendanceRequestCreatedResponse(attendanceRequest.getId(), attendanceRequest.getStatus());
    }

    private void sendCorrectionRequestPushSafely(Long workplaceId, Long userId) {
        try {
            List<OwnerPushTokenTarget> tokenList =
                    pushTokenMapper.findOwnerPushTokensByWorkplaceAndUserId(workplaceId, userId);
            if (CollectionUtils.isEmpty(tokenList)) {
                return;
            }

            List<FcmDto> fcmDtos = tokenList.stream()
                    .map(token -> FcmDto.builder()
                            .pushToken(token.getToken())
                            .title("\uD83C\uDF30 근무 정정 요청")
                            .content(token.getStaffName() + "님이 근무 정정을 요청했습니다.")
                            .build())
                    .collect(Collectors.toList());

            fcmService.sendMultiEachMessage(ProjectId.ALBAM.getMessage(), fcmDtos);
        } catch (Exception e) {
            log.warn("Correction request push send failed. workplaceId={}, userId={}, message={}",
                    workplaceId, userId, e.getMessage(), e);
        }
    }

    private String resolveNewRequestType(LocalDateTime checkIn, LocalDateTime checkOut, boolean isNewRecord) {
        if (isNewRecord) {
            return "NEW_RECORD";
        }
        if (checkIn != null && checkOut != null) {
            return "BOTH_EDIT";
        }
        if (checkIn != null) {
            return "CHECK_IN_EDIT";
        }
        return "CHECK_OUT_EDIT";
    }

    @Transactional
    public StaffWorkplaceSettingResponse getWorkplaceSetting(Long userId, Long workplaceId) {
        WorkplaceMember member = ensureActiveMember(workplaceId, userId);
        Workplace workplace = workplaceMapper.findById(workplaceId);
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        if (setting == null) {
            throw new ApiException(WORKPLACE_SETTING_NOT_FOUND);
        }
        return new StaffWorkplaceSettingResponse(
                workplaceId,
                workplace.getName(),
                workplace.getAddress(),
                workplace.getLatitude(),
                workplace.getLongitude(),
                workplace.getUseLocationRestriction(),
                workplace.getUseQrAttendance(),
                Boolean.TRUE.equals(member.getReceiveAttendancePush()),
                resolveHourlyWage(member, setting),
                setting.getSalaryCalcUnit(),
                setting.getDefaultCheckInTime(),
                setting.getDefaultCheckOutTime(),
                member.getBreakMinutes()
        );
    }

    public void updateMyName(Long userId, String name) {
        String trimmed = name.trim();
        String profileInitial = trimmed.isEmpty() ? "?" : trimmed.substring(0, 1);
        userMapper.updateName(userId, trimmed, profileInitial);
    }

    public void updateMyBreakMinutes(Long userId, Long workplaceId, Integer breakMinutes) {
        WorkplaceMember member = ensureActiveMember(workplaceId, userId);
        workplaceMemberMapper.updateBreakMinutes(member.getId(), breakMinutes);
    }

    private WorkplaceMember ensureActiveMember(Long workplaceId, Long userId) {
        WorkplaceMember member = workplaceMemberMapper.findActiveMember(workplaceId, userId);
        if (member == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, ACTIVE_WORKPLACE_MEMBER_NOT_FOUND);
        }
        return member;
    }

    private int calculateWorkedMinutes(LocalDateTime from, LocalDateTime to) {
        return (int) Math.max(Duration.between(from, to).toMinutes(), 0);
    }

    private int safeMinutes(Integer workedMinutes) {
        return workedMinutes == null ? 0 : workedMinutes;
    }

    private BigDecimal safeWage(BigDecimal wage) {
        return wage == null ? BigDecimal.ZERO : wage;
    }

    private YearMonth parseYearMonth(String yearMonth) {
        try {
            return YearMonth.parse(yearMonth);
        } catch (DateTimeParseException e) {
            throw new ApiException(INVALID_REQUEST);
        }
    }

    private String resolveWageType(WorkplaceMember member) {
        return (member != null && member.getWageType() != null) ? member.getWageType() : "HOURLY";
    }

    private BigDecimal resolveMonthlyWage(WorkplaceMember member) {
        return (member != null && member.getMonthlyWage() != null) ? member.getMonthlyWage() : BigDecimal.ZERO;
    }

    private BigDecimal resolveHourlyWage(WorkplaceMember member, WorkplaceSetting setting) {
        if (member != null && member.getHourlyWage() != null) {
            return member.getHourlyWage();
        }
        if (setting != null && setting.getDefaultHourlyWage() != null) {
            return setting.getDefaultHourlyWage();
        }
        return BigDecimal.ZERO;
    }

    private List<WorkplaceBreakPolicy> resolveBreakPolicies(Long workplaceId, WorkplaceSetting setting) {
        if (setting == null || !Boolean.TRUE.equals(setting.getUseBreakPolicy())) {
            return List.of();
        }
        return workplaceBreakPolicyMapper.findAllByWorkplaceId(workplaceId);
    }

    public List<StaffPayslipListItemResponse> getMyPayslips(Long userId, Long workplaceId, LocalDate fromDate, LocalDate toDate) {
        return payslipMapper.findByUserId(userId, workplaceId, fromDate, toDate).stream()
                .map(p -> {
                    BigDecimal whp = p.getWeeklyHolidayPay() != null ? p.getWeeklyHolidayPay() : BigDecimal.ZERO;
                    return new StaffPayslipListItemResponse(
                            p.getId(), p.getWorkplaceId(), p.getWorkplaceName(),
                            p.getFromDate(), p.getToDate(), p.getCreatedAt().toLocalDate(),
                            p.getWorkedDays(), p.getWorkedMinutes(), p.getWageType(), p.getHourlyWage(), p.getMonthlyWage(),
                            p.getBaseWage(), whp, p.getBonusAmount(), p.getDeductionAmount(), p.getTotalWage()
                    );
                })
                .collect(Collectors.toList());
    }

    public StaffPayslipDetailResponse getMyPayslipDetail(Long userId, Long payslipId) {
        Payslip payslip = payslipMapper.findById(payslipId);
        if (payslip == null || !userId.equals(payslip.getUserId()) || !"CONFIRMED".equals(payslip.getStatus())) {
            throw new ApiException(HttpStatus.NOT_FOUND, PAYSLIP_NOT_FOUND);
        }
        List<PayslipDeductionItemResponse> deductions = payslipDeductionMapper.findByPayslipId(payslipId).stream()
                .map(d -> new PayslipDeductionItemResponse(d.getId(), d.getDeductionType(), d.getName(), d.getAmount(), d.getNote(), d.getDisplayOrder()))
                .collect(Collectors.toList());
        List<PayslipRecordItem> records = deserializeSnapshot(payslip.getDailySnapshot());
        BigDecimal whp = payslip.getWeeklyHolidayPay() != null ? payslip.getWeeklyHolidayPay() : BigDecimal.ZERO;
        return new StaffPayslipDetailResponse(
                payslip.getId(), payslip.getWorkplaceId(), payslip.getWorkplaceName(),
                payslip.getFromDate(), payslip.getToDate(), payslip.getCreatedAt().toLocalDate(),
                payslip.getWorkedDays(), payslip.getWorkedMinutes(), payslip.getWageType(), payslip.getHourlyWage(), payslip.getMonthlyWage(),
                payslip.getBaseWage(), whp, payslip.getBonusAmount(), payslip.getDeductionAmount(), payslip.getTotalWage(),
                payslip.getBonusNote(), deductions, records
        );
    }

    public List<MemberScheduleItemResponse> getMySchedules(Long userId, Long workplaceId) {
        WorkplaceMember member = workplaceMemberMapper.findActiveMember(workplaceId, userId);
        if (member == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, MEMBER_NOT_FOUND);
        }
        return workplaceMemberScheduleMapper.findByWorkplaceAndUser(workplaceId, userId).stream()
                .map(s -> new MemberScheduleItemResponse(s.getDayOfWeek(), s.getScheduledCheckInTime(), s.getScheduledCheckOutTime()))
                .collect(Collectors.toList());
    }

    private List<PayslipRecordItem> deserializeSnapshot(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<PayslipRecordItem>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
