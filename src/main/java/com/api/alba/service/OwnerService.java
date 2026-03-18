package com.api.alba.service;

import com.api.alba.domain.AttendanceRecord;
import com.api.alba.domain.AttendanceRequest;
import com.api.alba.domain.Workplace;
import com.api.alba.domain.WorkplaceMember;
import com.api.alba.dto.CreateWorkplaceRequest;
import com.api.alba.dto.DashboardTodayResponse;
import com.api.alba.dto.EmployeeWageSummary;
import com.api.alba.dto.InviteCodeResponse;
import com.api.alba.dto.OwnerDecisionRequest;
import com.api.alba.exception.ApiException;
import com.api.alba.mapper.AttendanceRecordMapper;
import com.api.alba.mapper.AttendanceRequestMapper;
import com.api.alba.mapper.UserMapper;
import com.api.alba.mapper.WorkplaceMapper;
import com.api.alba.mapper.WorkplaceMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OwnerService {
    private final WorkplaceMapper workplaceMapper;
    private final WorkplaceMemberMapper workplaceMemberMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final AttendanceRequestMapper attendanceRequestMapper;
    private final UserMapper userMapper;

    @Transactional
    public Workplace createWorkplace(Long ownerUserId, CreateWorkplaceRequest request) {
        validateOwnerUserType(ownerUserId);

        Workplace workplace = new Workplace();
        workplace.setOwnerId(ownerUserId);
        workplace.setName(request.getName());
        workplace.setAddress(request.getAddress());
        workplace.setInviteCode(generateInviteCode());
        workplaceMapper.insert(workplace);

        WorkplaceMember member = new WorkplaceMember();
        member.setWorkplaceId(workplace.getId());
        member.setUserId(ownerUserId);
        member.setRole("OWNER");
        member.setStatus("ACTIVE");
        member.setHourlyWage(null);
        workplaceMemberMapper.insert(member);

        return workplaceMapper.findById(workplace.getId());
    }

    public InviteCodeResponse getInviteCode(Long ownerUserId, Long workplaceId) {
        ensureOwner(workplaceId, ownerUserId);
        Workplace workplace = workplaceMapper.findById(workplaceId);
        if (workplace == null) {
            throw new ApiException("Workplace not found.");
        }
        return new InviteCodeResponse(workplace.getId(), workplace.getInviteCode());
    }

    public DashboardTodayResponse getTodayDashboard(Long ownerUserId, Long workplaceId) {
        ensureOwner(workplaceId, ownerUserId);
        LocalDate today = LocalDate.now();
        int checkedIn = attendanceRecordMapper.countTodayCheckedIn(workplaceId, today);
        int working = attendanceRecordMapper.countTodayWorking(workplaceId, today);
        return new DashboardTodayResponse(checkedIn, working);
    }

    public List<AttendanceRecord> getWorkplaceAttendanceRecords(
            Long ownerUserId,
            Long workplaceId,
            Long userId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        ensureOwner(workplaceId, ownerUserId);
        if (fromDate.isAfter(toDate)) {
            throw new ApiException("fromDate must be earlier than or equal to toDate.");
        }
        return attendanceRecordMapper.findWorkplaceRecordsByPeriod(workplaceId, userId, fromDate, toDate);
    }

    @Transactional
    public void decideAttendanceRequest(Long ownerUserId, Long requestId, OwnerDecisionRequest request) {
        AttendanceRequest attendanceRequest = attendanceRequestMapper.findById(requestId);
        if (attendanceRequest == null) {
            throw new ApiException("Attendance request not found.");
        }
        if (!"PENDING".equals(attendanceRequest.getStatus())) {
            throw new ApiException("Only pending requests can be processed.");
        }

        AttendanceRecord record = attendanceRecordMapper.findById(attendanceRequest.getAttendanceRecordId());
        if (record == null) {
            throw new ApiException("Attendance record not found.");
        }

        WorkplaceMember ownerMember = ensureOwner(record.getWorkplaceId(), ownerUserId);
        if (ownerMember == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only owner can process request.");
        }

        String status = request.getStatus().toUpperCase();
        if ("APPROVED".equals(status)) {
            applyApprovedRequest(record, attendanceRequest);
        }
        attendanceRequestMapper.updateStatus(requestId, status);
    }

    public List<EmployeeWageSummary> getExpectedWageSummary(
            Long ownerUserId,
            Long workplaceId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        ensureOwner(workplaceId, ownerUserId);
        if (fromDate.isAfter(toDate)) {
            throw new ApiException("fromDate must be earlier than or equal to toDate.");
        }
        return attendanceRecordMapper.findEmployeeWageSummaryByPeriod(workplaceId, fromDate, toDate);
    }

    private void applyApprovedRequest(AttendanceRecord record, AttendanceRequest request) {
        LocalDateTime newCheckIn = request.getRequestedCheckInAt() != null ? request.getRequestedCheckInAt() : record.getCheckInAt();
        LocalDateTime newCheckOut = request.getRequestedCheckOutAt() != null ? request.getRequestedCheckOutAt() : record.getCheckOutAt();

        int workedMinutes = 0;
        if (newCheckIn != null && newCheckOut != null) {
            workedMinutes = (int) Math.max(Duration.between(newCheckIn, newCheckOut).toMinutes(), 0);
        }

        WorkplaceMember staffMember = workplaceMemberMapper.findActiveMember(record.getWorkplaceId(), record.getUserId());
        BigDecimal hourlyWage = (staffMember == null || staffMember.getHourlyWage() == null)
                ? BigDecimal.ZERO
                : staffMember.getHourlyWage();
        BigDecimal wage = hourlyWage
                .multiply(BigDecimal.valueOf(workedMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        String attendanceStatus = newCheckOut == null ? "WORKING" : "COMPLETED";
        attendanceRecordMapper.updateByOwnerDecision(
                record.getId(),
                newCheckIn,
                newCheckOut,
                workedMinutes,
                wage,
                wage,
                attendanceStatus
        );
    }

    private WorkplaceMember ensureOwner(Long workplaceId, Long userId) {
        WorkplaceMember ownerMember = workplaceMemberMapper.findActiveOwnerMember(workplaceId, userId);
        if (ownerMember == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Owner access only.");
        }
        return ownerMember;
    }

    private String generateInviteCode() {
        String token = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return token.substring(0, 10);
    }

    private void validateOwnerUserType(Long userId) {
        com.api.alba.domain.User user = userMapper.findById(userId);
        if (user == null) {
            throw new ApiException("User not found.");
        }
        if (!"OWNER".equalsIgnoreCase(user.getUserType())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only OWNER user type can create workplace.");
        }
    }
}
