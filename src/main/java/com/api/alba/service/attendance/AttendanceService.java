package com.api.alba.service.attendance;

import com.api.alba.component.WageCalculationHelper;
import com.api.alba.component.WageCalculationHelper.WageCalculationResult;
import com.api.alba.domain.attendance.AttendanceRecord;
import com.api.alba.domain.log.ApiErrorLog;
import com.api.alba.dto.attendance.AutoCheckOutTarget;
import com.api.alba.domain.owner.Workplace;
import com.api.alba.domain.settings.WorkplaceBreakPolicy;
import com.api.alba.domain.settings.WorkplaceSetting;
import com.api.alba.domain.staff.WorkplaceMember;
import com.api.alba.dto.attendance.AttendanceCheckInRequest;
import com.api.alba.dto.attendance.AttendanceCheckOutRequest;
import com.api.alba.dto.attendance.QrAttendanceRequest;
import com.api.alba.security.JwtTokenProvider;
import com.api.alba.dto.push.OwnerPushTokenTarget;
import com.api.alba.exception.ApiException;
import com.api.alba.firebase.FcmDto;
import com.api.alba.firebase.FcmService;
import com.api.alba.firebase.ProjectId;
import com.api.alba.mapper.attendance.AttendanceRecordMapper;
import com.api.alba.mapper.owner.WorkplaceMapper;
import com.api.alba.mapper.push.PushTokenMapper;
import com.api.alba.mapper.settings.WorkplaceBreakPolicyMapper;
import com.api.alba.mapper.settings.WorkplaceSettingMapper;
import com.api.alba.domain.staff.WorkplaceMemberSchedule;
import com.api.alba.mapper.staff.WorkplaceMemberMapper;
import com.api.alba.mapper.staff.WorkplaceMemberScheduleMapper;
import com.api.alba.service.log.ApiErrorLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.api.alba.exception.ExceptionMessages.ACTIVE_WORKPLACE_MEMBER_NOT_FOUND;
import static com.api.alba.exception.ExceptionMessages.ALREADY_CHECKED_IN_FOR_DATE;
import static com.api.alba.exception.ExceptionMessages.ALREADY_CHECKED_OUT_FOR_DATE;
import static com.api.alba.exception.ExceptionMessages.CHECK_IN_RECORD_NOT_FOUND_FOR_DATE;
import static com.api.alba.exception.ExceptionMessages.INVALID_DATE_RANGE;
import static com.api.alba.exception.ExceptionMessages.LAT_LON_MUST_BE_PROVIDED_TOGETHER;
import static com.api.alba.exception.ExceptionMessages.LAT_LON_REQUIRED;
import static com.api.alba.exception.ExceptionMessages.OUTSIDE_ALLOWED_WORKPLACE_RADIUS;
import static com.api.alba.exception.ExceptionMessages.QR_TOKEN_INVALID;
import static com.api.alba.exception.ExceptionMessages.WORKPLACE_LOCATION_NOT_CONFIGURED;
import static com.api.alba.exception.ExceptionMessages.WORKPLACE_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private static final int DEFAULT_ALLOWED_RADIUS_METERS = 100;

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final WorkplaceMemberMapper workplaceMemberMapper;
    private final WorkplaceMemberScheduleMapper workplaceMemberScheduleMapper;
    private final WorkplaceMapper workplaceMapper;
    private final WorkplaceSettingMapper workplaceSettingMapper;
    private final WorkplaceBreakPolicyMapper workplaceBreakPolicyMapper;
    private final PushTokenMapper pushTokenMapper;
    private final FcmService fcmService;
    private final WageCalculationHelper wageCalculationHelper;
    private final ApiErrorLogService apiErrorLogService;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AttendanceRecord checkIn(Long userId, AttendanceCheckInRequest request) {
        WorkplaceMember member = validateActiveMember(request.getWorkplaceId(), userId);
        validateGeofence(member.getWorkplaceId(), userId, request.getLatitude(), request.getLongitude(), request.getGpsAccuracy());
        LocalDate workDate = request.getWorkDate() == null ? LocalDate.now() : request.getWorkDate();

        AttendanceRecord existing = attendanceRecordMapper.findByWorkplaceUserAndDate(
                member.getWorkplaceId(),
                userId,
                workDate
        );
        if (existing != null) {
            throw new ApiException(ALREADY_CHECKED_IN_FOR_DATE);
        }

        AttendanceRecord record = new AttendanceRecord();
        record.setWorkplaceId(member.getWorkplaceId());
        record.setUserId(userId);
        record.setWorkDate(workDate);
        record.setCheckInAt(LocalDateTime.now());
        record.setStatus("WORKING");
        record.setWorkedMinutes(0);
        record.setBaseWage(BigDecimal.ZERO);
        record.setFinalWage(BigDecimal.ZERO);
        record.setNote(request.getNote());

        try {
            attendanceRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            throw new ApiException(ALREADY_CHECKED_IN_FOR_DATE);
        }

        sendAttendancePushSafely(member.getWorkplaceId(), userId, "출근 완료 알림", "%s님 출근이 완료되었습니다.");

        return attendanceRecordMapper.findByWorkplaceUserAndDate(record.getWorkplaceId(), userId, workDate);
    }

    @Transactional
    public AttendanceRecord checkOut(Long userId, AttendanceCheckOutRequest request) {
        WorkplaceMember member = validateActiveMember(request.getWorkplaceId(), userId);
        validateGeofence(member.getWorkplaceId(), userId, request.getLatitude(), request.getLongitude(), request.getGpsAccuracy());
        LocalDate workDate = request.getWorkDate() == null ? LocalDate.now() : request.getWorkDate();

        AttendanceRecord record = attendanceRecordMapper.findByWorkplaceUserAndDate(
                member.getWorkplaceId(),
                userId,
                workDate
        );
        // 해당 날짜 기록이 없으면 전날(야간 근무 케이스) fallback
        if (record == null || record.getCheckInAt() == null) {
            LocalDate prevDate = workDate.minusDays(1);
            AttendanceRecord prevRecord = attendanceRecordMapper.findByWorkplaceUserAndDate(
                    member.getWorkplaceId(),
                    userId,
                    prevDate
            );
            if (prevRecord != null && prevRecord.getCheckInAt() != null && prevRecord.getCheckOutAt() == null) {
                record = prevRecord;
                workDate = prevDate;
            }
        }
        if (record == null || record.getCheckInAt() == null) {
            throw new ApiException(CHECK_IN_RECORD_NOT_FOUND_FOR_DATE);
        }
        if (record.getCheckOutAt() != null) {
            throw new ApiException(ALREADY_CHECKED_OUT_FOR_DATE);
        }

        LocalDateTime checkOutAt = LocalDateTime.now();
        long diff = Duration.between(record.getCheckInAt(), checkOutAt).toMinutes();
        int grossWorkedMinutes = (int) Math.max(diff, 0);

        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(member.getWorkplaceId());
        List<WorkplaceBreakPolicy> breakPolicies = resolveBreakPolicies(member.getWorkplaceId(), setting);
        BigDecimal hourlyWage = "MONTHLY".equals(member.getWageType()) ? BigDecimal.ZERO : resolveHourlyWage(member, setting);
        WageCalculationResult wageCalculation = wageCalculationHelper.calculate(
                hourlyWage,
                grossWorkedMinutes,
                setting,
                breakPolicies,
                member.getBreakMinutes()
        );

        LocalTime scheduledCheckInTime = resolveScheduledCheckInTime(member, record.getCheckInAt());
        String finalStatus = resolveCheckOutStatus(record.getCheckInAt(), setting, scheduledCheckInTime);
        attendanceRecordMapper.updateCheckOut(
                record.getId(),
                checkOutAt,
                wageCalculation.workedMinutes(),
                wageCalculation.baseWage(),
                wageCalculation.finalWage(),
                finalStatus
        );

        sendAttendancePushSafely(member.getWorkplaceId(), userId, "퇴근 완료 알림", "%s님 퇴근이 완료되었습니다.");

        return attendanceRecordMapper.findByWorkplaceUserAndDate(member.getWorkplaceId(), userId, workDate);
    }

    public List<AttendanceRecord> myRecords(Long userId, Long workplaceId, LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new ApiException(INVALID_DATE_RANGE);
        }
        validateActiveMember(workplaceId, userId);
        return attendanceRecordMapper.findMyRecordsByPeriod(workplaceId, userId, fromDate, toDate);
    }

    private LocalTime resolveScheduledCheckInTime(WorkplaceMember member, LocalDateTime checkInAt) {
        int dayOfWeek = checkInAt.getDayOfWeek().getValue();
        List<WorkplaceMemberSchedule> schedules = workplaceMemberScheduleMapper.findByWorkplaceAndUser(
                member.getWorkplaceId(), member.getUserId()
        );
        return schedules.stream()
                .filter(s -> s.getDayOfWeek() != null && s.getDayOfWeek() == dayOfWeek
                        && s.getScheduledCheckInTime() != null)
                .map(WorkplaceMemberSchedule::getScheduledCheckInTime)
                .findFirst()
                .orElse(null);
    }

    private String resolveCheckOutStatus(LocalDateTime checkInAt, WorkplaceSetting setting, LocalTime scheduledCheckInTime) {
        // 직원별 스케줄 출근 시간이 없으면 지각 판정 불가 → COMPLETED
        if (scheduledCheckInTime == null) {
            return "COMPLETED";
        }
        int graceMinutes = setting != null && setting.getLateGraceMinutes() != null ? setting.getLateGraceMinutes() : 0;
        LocalDateTime deadline = checkInAt.toLocalDate()
                .atTime(scheduledCheckInTime)
                .plusMinutes(graceMinutes);
        return checkInAt.isAfter(deadline) ? "LATE" : "COMPLETED";
    }

    private WorkplaceMember validateActiveMember(Long workplaceId, Long userId) {
        WorkplaceMember member = workplaceMemberMapper.findActiveMember(workplaceId, userId);
        if (member == null) {
            throw new ApiException(ACTIVE_WORKPLACE_MEMBER_NOT_FOUND);
        }
        return member;
    }

    private static final int MAX_GPS_ACCURACY_BUFFER = 50;

    private void validateGeofence(Long workplaceId, Long userId, Double latitude, Double longitude, Double gpsAccuracy) {
        Workplace workplace = workplaceMapper.findById(workplaceId);
        if (workplace == null) {
            throw new ApiException(WORKPLACE_NOT_FOUND);
        }
        if (!Boolean.TRUE.equals(workplace.getUseLocationRestriction())) {
            return;
        }

        if (latitude == null && longitude == null) {
            throw new ApiException(LAT_LON_REQUIRED);
        }
        if (latitude == null || longitude == null) {
            throw new ApiException(LAT_LON_MUST_BE_PROVIDED_TOGETHER);
        }
        if (workplace.getLatitude() == null || workplace.getLongitude() == null) {
            throw new ApiException(WORKPLACE_LOCATION_NOT_CONFIGURED);
        }

        int allowedRadiusMeters = workplace.getAllowedRadiusMeters() == null
                ? DEFAULT_ALLOWED_RADIUS_METERS
                : workplace.getAllowedRadiusMeters();
        int accuracyBuffer = gpsAccuracy == null ? 0 : Math.min(gpsAccuracy.intValue(), MAX_GPS_ACCURACY_BUFFER);
        double distanceMeters = distanceMeters(
                workplace.getLatitude(),
                workplace.getLongitude(),
                latitude,
                longitude
        );

        if (distanceMeters > allowedRadiusMeters + accuracyBuffer) {
            apiErrorLogService.insert(ApiErrorLog.builder()
                    .workplaceId(workplaceId)
                    .userId(userId)
                    .controller("AttendanceService#validateGeofence")
                    .requestParams(String.format(
                            "{\"userLat\":%s,\"userLon\":%s,\"workplaceLat\":%s,\"workplaceLon\":%s,\"distanceMeters\":%.2f,\"allowedRadiusMeters\":%d,\"accuracyBuffer\":%d}",
                            latitude, longitude,
                            workplace.getLatitude(), workplace.getLongitude(),
                            distanceMeters, allowedRadiusMeters, accuracyBuffer))
                    .errorMessage(OUTSIDE_ALLOWED_WORKPLACE_RADIUS)
                    .build());
            throw new ApiException(HttpStatus.FORBIDDEN, String.format(
                    "%s (현재 근무지로부터 약 %dm, 허용 반경: %dm)",
                    OUTSIDE_ALLOWED_WORKPLACE_RADIUS, Math.round(distanceMeters), allowedRadiusMeters));
        }
    }

    private double distanceMeters(double fromLat, double fromLon, double toLat, double toLon) {
        double dLat = Math.toRadians(toLat - fromLat);
        double dLon = Math.toRadians(toLon - fromLon);

        double fromLatRad = Math.toRadians(fromLat);
        double toLatRad = Math.toRadians(toLat);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(fromLatRad) * Math.cos(toLatRad) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
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

    @Transactional
    public void autoCheckOutMissed(LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        List<AutoCheckOutTarget> targets = attendanceRecordMapper.findMissingCheckOutWithSchedule(
                today, today.getDayOfWeek().getValue(),
                yesterday, yesterday.getDayOfWeek().getValue()
        );

        if (targets.isEmpty()) {
            log.info("[자동 퇴근] 처리 대상 없음 (today={})", today);
            return;
        }

        log.info("[자동 퇴근] 처리 대상 {}건 (today={})", targets.size(), today);
        for (AutoCheckOutTarget target : targets) {
            try {
                LocalDate workDate = target.getCheckInAt().toLocalDate();
                LocalDate checkOutDate = target.isOvernight() ? workDate.plusDays(1) : workDate;
                LocalDateTime checkOutAt = checkOutDate.atTime(target.getScheduledCheckOutTime());
                long diffMinutes = Duration.between(target.getCheckInAt(), checkOutAt).toMinutes();
                int grossWorkedMinutes = (int) Math.max(diffMinutes, 0);

                WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(target.getWorkplaceId());
                List<WorkplaceBreakPolicy> breakPolicies = resolveBreakPolicies(target.getWorkplaceId(), setting);

                BigDecimal hourlyWage = "MONTHLY".equals(target.getWageType())
                        ? BigDecimal.ZERO
                        : resolveHourlyWageFromTarget(target, setting);

                WageCalculationResult wageResult = wageCalculationHelper.calculate(
                        hourlyWage, grossWorkedMinutes, setting, breakPolicies, target.getBreakMinutes()
                );

                String status = resolveCheckOutStatus(target.getCheckInAt(), setting, target.getScheduledCheckInTime());

                attendanceRecordMapper.updateCheckOut(
                        target.getRecordId(), checkOutAt,
                        wageResult.workedMinutes(), wageResult.baseWage(), wageResult.finalWage(),
                        status
                );
                log.info("[자동 퇴근] 처리 완료 recordId={}, workplaceId={}, userId={}, checkOutAt={}",
                        target.getRecordId(), target.getWorkplaceId(), target.getUserId(), checkOutAt);
            } catch (Exception e) {
                log.warn("[자동 퇴근] 처리 실패 recordId={}, workplaceId={}, userId={}, message={}",
                        target.getRecordId(), target.getWorkplaceId(), target.getUserId(), e.getMessage(), e);
            }
        }
    }

    private BigDecimal resolveHourlyWageFromTarget(AutoCheckOutTarget target, WorkplaceSetting setting) {
        if (target.getHourlyWage() != null) return target.getHourlyWage();
        if (setting != null && setting.getDefaultHourlyWage() != null) return setting.getDefaultHourlyWage();
        return BigDecimal.ZERO;
    }

    @Transactional
    public AttendanceRecord attendanceByQr(Long userId, QrAttendanceRequest request) {
        Long workplaceId = jwtTokenProvider.validateQrToken(request.getToken());
        if (workplaceId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, QR_TOKEN_INVALID);
        }

        WorkplaceMember member = validateActiveMember(workplaceId, userId);

        if ("IN".equals(request.getType())) {
            LocalDate workDate = LocalDate.now();
            AttendanceRecord existing = attendanceRecordMapper.findByWorkplaceUserAndDate(workplaceId, userId, workDate);
            if (existing != null) {
                throw new ApiException(ALREADY_CHECKED_IN_FOR_DATE);
            }
            AttendanceRecord record = new AttendanceRecord();
            record.setWorkplaceId(workplaceId);
            record.setUserId(userId);
            record.setWorkDate(workDate);
            record.setCheckInAt(LocalDateTime.now());
            record.setStatus("WORKING");
            record.setWorkedMinutes(0);
            record.setBaseWage(BigDecimal.ZERO);
            record.setFinalWage(BigDecimal.ZERO);
            try {
                attendanceRecordMapper.insert(record);
            } catch (DuplicateKeyException e) {
                throw new ApiException(ALREADY_CHECKED_IN_FOR_DATE);
            }
            sendAttendancePushSafely(workplaceId, userId, "출근 완료 알림", "%s님 출근이 완료되었습니다.");
            return attendanceRecordMapper.findByWorkplaceUserAndDate(workplaceId, userId, workDate);
        } else {
            LocalDate workDate = LocalDate.now();
            AttendanceRecord record = attendanceRecordMapper.findByWorkplaceUserAndDate(workplaceId, userId, workDate);
            if (record == null || record.getCheckInAt() == null) {
                LocalDate prevDate = workDate.minusDays(1);
                AttendanceRecord prevRecord = attendanceRecordMapper.findByWorkplaceUserAndDate(workplaceId, userId, prevDate);
                if (prevRecord != null && prevRecord.getCheckInAt() != null && prevRecord.getCheckOutAt() == null) {
                    record = prevRecord;
                    workDate = prevDate;
                }
            }
            if (record == null || record.getCheckInAt() == null) {
                throw new ApiException(CHECK_IN_RECORD_NOT_FOUND_FOR_DATE);
            }
            if (record.getCheckOutAt() != null) {
                throw new ApiException(ALREADY_CHECKED_OUT_FOR_DATE);
            }
            LocalDateTime checkOutAt = LocalDateTime.now();
            long diff = Duration.between(record.getCheckInAt(), checkOutAt).toMinutes();
            int grossWorkedMinutes = (int) Math.max(diff, 0);
            WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
            List<WorkplaceBreakPolicy> breakPolicies = resolveBreakPolicies(workplaceId, setting);
            BigDecimal hourlyWage = "MONTHLY".equals(member.getWageType()) ? BigDecimal.ZERO : resolveHourlyWage(member, setting);
            WageCalculationResult wageCalculation = wageCalculationHelper.calculate(hourlyWage, grossWorkedMinutes, setting, breakPolicies, member.getBreakMinutes());
            LocalTime scheduledCheckInTime = resolveScheduledCheckInTime(member, record.getCheckInAt());
            String finalStatus = resolveCheckOutStatus(record.getCheckInAt(), setting, scheduledCheckInTime);
            attendanceRecordMapper.updateCheckOut(record.getId(), checkOutAt, wageCalculation.workedMinutes(), wageCalculation.baseWage(), wageCalculation.finalWage(), finalStatus);
            sendAttendancePushSafely(workplaceId, userId, "퇴근 완료 알림", "%s님 퇴근이 완료되었습니다.");
            return attendanceRecordMapper.findByWorkplaceUserAndDate(workplaceId, userId, workDate);
        }
    }

    private void sendAttendancePushSafely(Long workplaceId, Long userId, String title, String contentFormat) {
        try {
            List<OwnerPushTokenTarget> tokenList =
                    pushTokenMapper.findOwnerPushTokensByWorkplaceAndUserId(workplaceId, userId);
            if (CollectionUtils.isEmpty(tokenList)) {
                return;
            }

            List<FcmDto> fcmDtos = tokenList.stream()
                    .map(token -> FcmDto.builder()
                            .pushToken(token.getToken())
                            .title(title)
                            .content(String.format(contentFormat, token.getStaffName()))
                            .build())
                    .collect(Collectors.toList());

            fcmService.sendMultiEachMessage(ProjectId.ALBAM.getMessage(), fcmDtos);
        } catch (Exception e) {
            log.warn("Attendance push send failed. workplaceId={}, userId={}, message={}",
                    workplaceId, userId, e.getMessage(), e);
        }
    }
}
