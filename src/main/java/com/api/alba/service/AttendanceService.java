package com.api.alba.service;

import com.api.alba.domain.AttendanceRecord;
import com.api.alba.domain.WorkplaceMember;
import com.api.alba.dto.AttendanceCheckInRequest;
import com.api.alba.dto.AttendanceCheckOutRequest;
import com.api.alba.exception.ApiException;
import com.api.alba.mapper.AttendanceRecordMapper;
import com.api.alba.mapper.WorkplaceMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
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
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final WorkplaceMemberMapper workplaceMemberMapper;

    @Transactional
    public AttendanceRecord checkIn(Long userId, AttendanceCheckInRequest request) {
        WorkplaceMember member = validateActiveMember(request.getWorkplaceId(), userId);
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

        BigDecimal hourlyWage = member.getHourlyWage() == null ? BigDecimal.ZERO : member.getHourlyWage();
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
}
