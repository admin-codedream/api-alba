package com.api.alba.service.staff;

import com.api.alba.component.WageCalculationHelper;
import com.api.alba.domain.attendance.AttendanceRecord;
import com.api.alba.domain.attendance.AttendanceRequest;
import com.api.alba.domain.owner.Workplace;
import com.api.alba.domain.settings.WorkplaceBreakPolicy;
import com.api.alba.domain.settings.WorkplaceSetting;
import com.api.alba.domain.staff.WorkplaceMember;
import com.api.alba.dto.attendance.AttendanceCorrectionRequestCreateRequest;
import com.api.alba.dto.attendance.AttendanceNewRecordRequestCreateRequest;
import com.api.alba.dto.attendance.AttendanceRequestCreatedResponse;
import com.api.alba.dto.staff.StaffAttendanceRequestListItemResponse;
import com.api.alba.dto.staff.JoinWorkplaceRequest;
import com.api.alba.dto.staff.JoinWorkplaceResponse;
import com.api.alba.dto.staff.MyAggregateSummary;
import com.api.alba.dto.staff.StaffHomeTodayResponse;
import com.api.alba.dto.staff.StaffMonthlyCalendarItemResponse;
import com.api.alba.dto.staff.StaffTodaySummaryResponse;
import com.api.alba.dto.staff.StaffWorkDetailResponse;
import com.api.alba.exception.ApiException;
import com.api.alba.mapper.attendance.AttendanceRecordMapper;
import com.api.alba.mapper.attendance.AttendanceRequestMapper;
import com.api.alba.mapper.owner.WorkplaceMapper;
import com.api.alba.mapper.settings.WorkplaceBreakPolicyMapper;
import com.api.alba.mapper.settings.WorkplaceSettingMapper;
import com.api.alba.mapper.staff.WorkplaceMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

import static com.api.alba.exception.ExceptionMessages.*;

@Service
@RequiredArgsConstructor
public class StaffService {
    private final WorkplaceMapper workplaceMapper;
    private final WorkplaceMemberMapper workplaceMemberMapper;
    private final WorkplaceSettingMapper workplaceSettingMapper;
    private final WorkplaceBreakPolicyMapper workplaceBreakPolicyMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final AttendanceRequestMapper attendanceRequestMapper;
    private final WageCalculationHelper wageCalculationHelper;

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

        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        List<WorkplaceBreakPolicy> breakPolicies = resolveBreakPolicies(workplaceId, setting);
        BigDecimal hourlyWage = resolveHourlyWage(member, setting);
        if (record == null) {
            return new StaffHomeTodayResponse(
                    workplaceId,
                    today,
                    "BEFORE_CHECK_IN",
                    null,
                    null,
                    0,
                    BigDecimal.ZERO,
                    hourlyWage
            );
        }

        int workedMinutes = safeMinutes(record.getWorkedMinutes());
        BigDecimal expectedWage = safeWage(record.getFinalWage());
        if (record.getCheckInAt() != null && record.getCheckOutAt() == null) {
            int grossWorkedMinutes = calculateWorkedMinutes(record.getCheckInAt(), LocalDateTime.now());
            workedMinutes = wageCalculationHelper.calculatePayableWorkedMinutes(grossWorkedMinutes, setting, breakPolicies);
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
                hourlyWage
        );
    }

    public StaffTodaySummaryResponse getTodaySummary(Long userId, Long workplaceId) {
        WorkplaceMember member = ensureActiveMember(workplaceId, userId);
        LocalDate today = LocalDate.now();
        AttendanceRecord record = attendanceRecordMapper.findByWorkplaceUserAndDate(workplaceId, userId, today);

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
                        breakPolicies
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
                cumulativeExpectedWage
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

    @Transactional
    public AttendanceRequestCreatedResponse submitCorrectionRequest(
            Long userId,
            Long attendanceRecordId,
            AttendanceCorrectionRequestCreateRequest request
    ) {
        AttendanceRecord record = attendanceRecordMapper.findById(attendanceRecordId);
        if (record == null) {
            throw new ApiException(ATTENDANCE_RECORD_NOT_FOUND);
        }
        if (!record.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, CORRECTION_ONLY_FOR_OWN_RECORD);
        }
        ensureActiveMember(record.getWorkplaceId(), userId);

        String type = resolveRequestType(request);
        if (attendanceRequestMapper.countPendingByRecordAndUser(attendanceRecordId, userId) > 0) {
            throw new ApiException(PENDING_CORRECTION_REQUEST_EXISTS);
        }

        AttendanceRequest attendanceRequest = new AttendanceRequest();
        attendanceRequest.setAttendanceRecordId(attendanceRecordId);
        attendanceRequest.setUserId(userId);
        attendanceRequest.setType(type);
        attendanceRequest.setRequestedCheckInAt(request.getRequestedCheckInAt());
        attendanceRequest.setRequestedCheckOutAt(request.getRequestedCheckOutAt());
        attendanceRequest.setReason(request.getReason());
        attendanceRequest.setStatus("PENDING");
        attendanceRequestMapper.insert(attendanceRequest);

        return new AttendanceRequestCreatedResponse(attendanceRequest.getId(), attendanceRequest.getStatus());
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

        return new AttendanceRequestCreatedResponse(attendanceRequest.getId(), attendanceRequest.getStatus());
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

    private WorkplaceMember ensureActiveMember(Long workplaceId, Long userId) {
        WorkplaceMember member = workplaceMemberMapper.findActiveMember(workplaceId, userId);
        if (member == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, ACTIVE_WORKPLACE_MEMBER_NOT_FOUND);
        }
        return member;
    }

    private String resolveRequestType(AttendanceCorrectionRequestCreateRequest request) {
        LocalDateTime checkIn = request.getRequestedCheckInAt();
        LocalDateTime checkOut = request.getRequestedCheckOutAt();

        if (checkIn == null && checkOut == null) {
            throw new ApiException(AT_LEAST_ONE_REQUESTED_CHECK_IN_OR_OUT_REQUIRED);
        }
        if (checkIn != null && checkOut != null && checkOut.isBefore(checkIn)) {
            throw new ApiException(REQUESTED_CHECK_OUT_MUST_BE_LATER_THAN_CHECK_IN);
        }
        if (checkIn != null && checkOut != null) {
            return "BOTH_EDIT";
        }
        if (checkIn != null) {
            return "CHECK_IN_EDIT";
        }
        return "CHECK_OUT_EDIT";
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

    private BigDecimal resolveHourlyWage(WorkplaceMember member, WorkplaceSetting setting) {
        if (setting != null && setting.getDefaultHourlyWage() != null) {
            return setting.getDefaultHourlyWage();
        }
        return safeWage(member.getHourlyWage());
    }

    private List<WorkplaceBreakPolicy> resolveBreakPolicies(Long workplaceId, WorkplaceSetting setting) {
        if (setting == null || !Boolean.TRUE.equals(setting.getUseBreakPolicy())) {
            return List.of();
        }
        return workplaceBreakPolicyMapper.findAllByWorkplaceId(workplaceId);
    }
}
