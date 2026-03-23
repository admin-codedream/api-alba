package com.api.alba.service.staff;

import com.api.alba.domain.attendance.AttendanceRecord;
import com.api.alba.domain.attendance.AttendanceRequest;
import com.api.alba.domain.owner.Workplace;
import com.api.alba.domain.staff.WorkplaceMember;
import com.api.alba.domain.settings.WorkplaceSetting;
import com.api.alba.dto.attendance.AttendanceCorrectionRequestCreateRequest;
import com.api.alba.dto.attendance.AttendanceRequestCreatedResponse;
import com.api.alba.dto.staff.JoinWorkplaceRequest;
import com.api.alba.dto.staff.JoinWorkplaceResponse;
import com.api.alba.dto.staff.MyAggregateSummary;
import com.api.alba.dto.staff.StaffHomeTodayResponse;
import com.api.alba.dto.staff.StaffTodaySummaryResponse;
import com.api.alba.exception.ApiException;
import com.api.alba.mapper.attendance.AttendanceRecordMapper;
import com.api.alba.mapper.attendance.AttendanceRequestMapper;
import com.api.alba.mapper.owner.WorkplaceMapper;
import com.api.alba.mapper.staff.WorkplaceMemberMapper;
import com.api.alba.mapper.settings.WorkplaceSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import static com.api.alba.exception.ExceptionMessages.*;

@Service
@RequiredArgsConstructor
public class StaffService {
    private final WorkplaceMapper workplaceMapper;
    private final WorkplaceMemberMapper workplaceMemberMapper;
    private final WorkplaceSettingMapper workplaceSettingMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final AttendanceRequestMapper attendanceRequestMapper;

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

        BigDecimal hourlyWage = resolveHourlyWage(workplaceId, member);
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
            workedMinutes = calculateWorkedMinutes(record.getCheckInAt(), LocalDateTime.now());
            expectedWage = calculateWage(hourlyWage, workedMinutes);
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

        BigDecimal hourlyWage = resolveHourlyWage(workplaceId, member);
        int todayWorkedMinutes = 0;
        BigDecimal todayExpectedWage = BigDecimal.ZERO;
        if (record != null) {
            todayWorkedMinutes = safeMinutes(record.getWorkedMinutes());
            todayExpectedWage = safeWage(record.getFinalWage());

            if (record.getCheckInAt() != null && record.getCheckOutAt() == null) {
                todayWorkedMinutes = calculateWorkedMinutes(record.getCheckInAt(), LocalDateTime.now());
                todayExpectedWage = calculateWage(hourlyWage, todayWorkedMinutes);
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

    private WorkplaceMember ensureActiveMember(Long workplaceId, Long userId) {
        WorkplaceMember member = workplaceMemberMapper.findActiveMember(workplaceId, userId);
        if (member == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Active workplace member not found.");
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

    private BigDecimal calculateWage(BigDecimal hourlyWage, int workedMinutes) {
        return hourlyWage
                .multiply(BigDecimal.valueOf(workedMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private int safeMinutes(Integer workedMinutes) {
        return workedMinutes == null ? 0 : workedMinutes;
    }

    private BigDecimal safeWage(BigDecimal wage) {
        return wage == null ? BigDecimal.ZERO : wage;
    }

    private BigDecimal resolveHourlyWage(Long workplaceId, WorkplaceMember member) {
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        if (setting != null && setting.getDefaultHourlyWage() != null) {
            return setting.getDefaultHourlyWage();
        }
        return safeWage(member.getHourlyWage());
    }
}
