package com.api.alba.service.attendance;

import com.api.alba.domain.attendance.AttendanceRecord;
import com.api.alba.domain.owner.Workplace;
import com.api.alba.domain.staff.WorkplaceMember;
import com.api.alba.domain.settings.WorkplaceSetting;
import com.api.alba.dto.attendance.AttendanceCheckInRequest;
import com.api.alba.dto.attendance.AttendanceCheckOutRequest;
import com.api.alba.exception.ApiException;
import com.api.alba.mapper.attendance.AttendanceRecordMapper;
import com.api.alba.mapper.owner.WorkplaceMapper;
import com.api.alba.mapper.staff.WorkplaceMemberMapper;
import com.api.alba.mapper.settings.WorkplaceSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.api.alba.exception.ExceptionMessages.ACTIVE_WORKPLACE_MEMBER_NOT_FOUND;
import static com.api.alba.exception.ExceptionMessages.ALREADY_CHECKED_IN_FOR_DATE;
import static com.api.alba.exception.ExceptionMessages.ALREADY_CHECKED_OUT_FOR_DATE;
import static com.api.alba.exception.ExceptionMessages.CHECK_IN_RECORD_NOT_FOUND_FOR_DATE;
import static com.api.alba.exception.ExceptionMessages.INVALID_DATE_RANGE;
import static com.api.alba.exception.ExceptionMessages.LAT_LON_MUST_BE_PROVIDED_TOGETHER;
import static com.api.alba.exception.ExceptionMessages.LAT_LON_REQUIRED;
import static com.api.alba.exception.ExceptionMessages.OUTSIDE_ALLOWED_WORKPLACE_RADIUS;
import static com.api.alba.exception.ExceptionMessages.WORKPLACE_LOCATION_NOT_CONFIGURED;
import static com.api.alba.exception.ExceptionMessages.WORKPLACE_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private static final int DEFAULT_ALLOWED_RADIUS_METERS = 100;
    private static final BigDecimal TEN_WON_UNIT = BigDecimal.TEN;

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final WorkplaceMemberMapper workplaceMemberMapper;
    private final WorkplaceMapper workplaceMapper;
    private final WorkplaceSettingMapper workplaceSettingMapper;

    @Transactional
    public AttendanceRecord checkIn(Long userId, AttendanceCheckInRequest request) {
        WorkplaceMember member = validateActiveMember(request.getWorkplaceId(), userId);
        validateGeofence(member.getWorkplaceId(), request.getLatitude(), request.getLongitude());
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

        return attendanceRecordMapper.findByWorkplaceUserAndDate(record.getWorkplaceId(), userId, workDate);
    }

    @Transactional
    public AttendanceRecord checkOut(Long userId, AttendanceCheckOutRequest request) {
        WorkplaceMember member = validateActiveMember(request.getWorkplaceId(), userId);
        validateGeofence(member.getWorkplaceId(), request.getLatitude(), request.getLongitude());
        LocalDate workDate = request.getWorkDate() == null ? LocalDate.now() : request.getWorkDate();

        AttendanceRecord record = attendanceRecordMapper.findByWorkplaceUserAndDate(
                member.getWorkplaceId(),
                userId,
                workDate
        );
        if (record == null || record.getCheckInAt() == null) {
            throw new ApiException(CHECK_IN_RECORD_NOT_FOUND_FOR_DATE);
        }
        if (record.getCheckOutAt() != null) {
            throw new ApiException(ALREADY_CHECKED_OUT_FOR_DATE);
        }

        LocalDateTime checkOutAt = LocalDateTime.now();
        long diff = Duration.between(record.getCheckInAt(), checkOutAt).toMinutes();
        int workedMinutes = (int) Math.max(diff, 0);

        // 사업장별 시급
        BigDecimal hourlyWage = resolveHourlyWage(member);
        // 시급 * 근무시간 / 60
        BigDecimal baseWage = hourlyWage
                .multiply(BigDecimal.valueOf(workedMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        baseWage = truncateToTenWonUnit(baseWage);

        attendanceRecordMapper.updateCheckOut(
                record.getId(),
                checkOutAt,
                workedMinutes,
                baseWage,
                baseWage,
                "COMPLETED"
        );

        return attendanceRecordMapper.findByWorkplaceUserAndDate(member.getWorkplaceId(), userId, workDate);
    }

    public List<AttendanceRecord> myRecords(Long userId, Long workplaceId, LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new ApiException(INVALID_DATE_RANGE);
        }
        validateActiveMember(workplaceId, userId);
        return attendanceRecordMapper.findMyRecordsByPeriod(workplaceId, userId, fromDate, toDate);
    }

    private WorkplaceMember validateActiveMember(Long workplaceId, Long userId) {
        WorkplaceMember member = workplaceMemberMapper.findActiveMember(workplaceId, userId);
        if (member == null) {
            throw new ApiException(ACTIVE_WORKPLACE_MEMBER_NOT_FOUND);
        }
        return member;
    }

    private void validateGeofence(Long workplaceId, Double latitude, Double longitude) {
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
        double distanceMeters = distanceMeters(
                workplace.getLatitude(),
                workplace.getLongitude(),
                latitude,
                longitude
        );

        if (distanceMeters > allowedRadiusMeters) {
            throw new ApiException(HttpStatus.FORBIDDEN, OUTSIDE_ALLOWED_WORKPLACE_RADIUS);
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

    private BigDecimal resolveHourlyWage(WorkplaceMember member) {
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(member.getWorkplaceId());
        if (setting != null && setting.getDefaultHourlyWage() != null) {
            return setting.getDefaultHourlyWage();
        }
        return member.getHourlyWage() == null ? BigDecimal.ZERO : member.getHourlyWage();
    }

    private BigDecimal truncateToTenWonUnit(BigDecimal wage) {
        if (wage == null) {
            return BigDecimal.ZERO;
        }
        return wage
                .divide(TEN_WON_UNIT, 0, RoundingMode.DOWN)
                .multiply(TEN_WON_UNIT)
                .setScale(2, RoundingMode.DOWN);
    }
}
