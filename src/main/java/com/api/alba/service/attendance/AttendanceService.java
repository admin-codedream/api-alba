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

@Service
@RequiredArgsConstructor
public class AttendanceService {
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private static final int DEFAULT_ALLOWED_RADIUS_METERS = 100;

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
            throw new ApiException("Already checked in for this date.");
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
            throw new ApiException("Already checked in for this date.");
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
            throw new ApiException("Check-in record not found for this date.");
        }
        if (record.getCheckOutAt() != null) {
            throw new ApiException("Already checked out for this date.");
        }

        LocalDateTime checkOutAt = LocalDateTime.now();
        long diff = Duration.between(record.getCheckInAt(), checkOutAt).toMinutes();
        int workedMinutes = (int) Math.max(diff, 0);

        BigDecimal hourlyWage = resolveHourlyWage(member);
        BigDecimal baseWage = hourlyWage
                .multiply(BigDecimal.valueOf(workedMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

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
            throw new ApiException("fromDate must be earlier than or equal to toDate.");
        }
        validateActiveMember(workplaceId, userId);
        return attendanceRecordMapper.findMyRecordsByPeriod(workplaceId, userId, fromDate, toDate);
    }

    private WorkplaceMember validateActiveMember(Long workplaceId, Long userId) {
        WorkplaceMember member = workplaceMemberMapper.findActiveMember(workplaceId, userId);
        if (member == null) {
            throw new ApiException("Active workplace member not found.");
        }
        return member;
    }

    private void validateGeofence(Long workplaceId, Double latitude, Double longitude) {
        if (latitude == null && longitude == null) {
            return;
        }
        if (latitude == null || longitude == null) {
            throw new ApiException("latitude and longitude must be provided together.");
        }

        Workplace workplace = workplaceMapper.findById(workplaceId);
        if (workplace == null) {
            throw new ApiException("Workplace not found.");
        }
        if (workplace.getLatitude() == null || workplace.getLongitude() == null) {
            throw new ApiException("Workplace location is not configured.");
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
            throw new ApiException(HttpStatus.FORBIDDEN, "You are outside the allowed workplace radius.");
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
}
