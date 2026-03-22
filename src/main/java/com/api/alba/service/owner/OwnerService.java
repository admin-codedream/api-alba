package com.api.alba.service.owner;

import com.api.alba.domain.attendance.AttendanceRecord;
import com.api.alba.domain.attendance.AttendanceRequest;
import com.api.alba.domain.owner.Workplace;
import com.api.alba.domain.staff.WorkplaceMember;
import com.api.alba.domain.settings.WorkplaceSetting;
import com.api.alba.domain.auth.User;
import com.api.alba.dto.owner.CreateWorkplaceRequest;
import com.api.alba.dto.owner.DashboardTodayResponse;
import com.api.alba.dto.owner.AttendanceRequestListItemResponse;
import com.api.alba.dto.staff.EmployeeWageSummary;
import com.api.alba.dto.staff.InviteCodeResponse;
import com.api.alba.dto.owner.OwnerDecisionRequest;
import com.api.alba.dto.owner.UpdateWorkplaceHourlyWageRequest;
import com.api.alba.exception.ApiException;
import com.api.alba.mapper.attendance.AttendanceRecordMapper;
import com.api.alba.mapper.attendance.AttendanceRequestMapper;
import com.api.alba.mapper.auth.UserMapper;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OwnerService {
    private static final int DEFAULT_ALLOWED_RADIUS_METERS = 100;
    private static final boolean DEFAULT_USE_LOCATION_RESTRICTION = false;
    private static final BigDecimal DEFAULT_WORKPLACE_HOURLY_WAGE = BigDecimal.ZERO;
    private static final BigDecimal TEN_WON_UNIT = BigDecimal.TEN;

    private final WorkplaceMapper workplaceMapper;
    private final WorkplaceMemberMapper workplaceMemberMapper;
    private final WorkplaceSettingMapper workplaceSettingMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final AttendanceRequestMapper attendanceRequestMapper;
    private final UserMapper userMapper;

    @Transactional
    public Workplace createWorkplace(Long ownerUserId, CreateWorkplaceRequest request) {
        validateOwnerUserType(ownerUserId);
        validateWorkplaceLocationRequest(request);

        Workplace workplace = new Workplace();
        workplace.setOwnerId(ownerUserId);
        workplace.setName(request.getName());
        workplace.setAddress(request.getAddress());
        workplace.setInviteCode(generateInviteCode());
        workplace.setLatitude(request.getLatitude());
        workplace.setLongitude(request.getLongitude());
        workplace.setAllowedRadiusMeters(
                request.getAllowedRadiusMeters() == null
                        ? DEFAULT_ALLOWED_RADIUS_METERS
                        : request.getAllowedRadiusMeters()
        );
        workplace.setUseLocationRestriction(
                request.getUseLocationRestriction() == null
                        ? DEFAULT_USE_LOCATION_RESTRICTION
                        : request.getUseLocationRestriction()
        );
        workplaceMapper.insert(workplace);

        WorkplaceMember member = new WorkplaceMember();
        member.setWorkplaceId(workplace.getId());
        member.setUserId(ownerUserId);
        member.setRole("OWNER");
        member.setStatus("ACTIVE");
        member.setHourlyWage(null);
        workplaceMemberMapper.insert(member);

        WorkplaceSetting workplaceSetting = new WorkplaceSetting();
        workplaceSetting.setWorkplaceId(workplace.getId());
        workplaceSetting.setLateGraceMinutes(0);
        workplaceSetting.setSalaryCalcUnit("MINUTE");
        workplaceSetting.setRoundingPolicy("NONE");
        workplaceSetting.setDefaultHourlyWage(
                request.getHourlyWage() == null ? DEFAULT_WORKPLACE_HOURLY_WAGE : request.getHourlyWage()
        );
        workplaceSettingMapper.insert(workplaceSetting);

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
        int pendingAttendanceRequestCount = attendanceRequestMapper.countPendingByWorkplaceId(workplaceId);
        return new DashboardTodayResponse(checkedIn, working, pendingAttendanceRequestCount);
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

    public List<AttendanceRequestListItemResponse> getAttendanceRequests(
            Long ownerUserId,
            Long workplaceId,
            String status
    ) {
        ensureOwner(workplaceId, ownerUserId);
        String normalizedStatus = normalizeRequestStatus(status);
        return attendanceRequestMapper.findByWorkplaceId(workplaceId, normalizedStatus);
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

    @Transactional
    public void updateWorkplaceHourlyWage(
            Long ownerUserId,
            Long workplaceId,
            UpdateWorkplaceHourlyWageRequest request
    ) {
        ensureOwner(workplaceId, ownerUserId);
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        if (setting == null) {
            throw new ApiException("Workplace setting not found.");
        }
        workplaceSettingMapper.updateDefaultHourlyWage(workplaceId, request.getHourlyWage());
    }

    private void applyApprovedRequest(AttendanceRecord record, AttendanceRequest request) {
        LocalDateTime newCheckIn = request.getRequestedCheckInAt() != null ? request.getRequestedCheckInAt() : record.getCheckInAt();
        LocalDateTime newCheckOut = request.getRequestedCheckOutAt() != null ? request.getRequestedCheckOutAt() : record.getCheckOutAt();

        int workedMinutes = 0;
        if (newCheckIn != null && newCheckOut != null) {
            workedMinutes = (int) Math.max(Duration.between(newCheckIn, newCheckOut).toMinutes(), 0);
        }

        WorkplaceMember staffMember = workplaceMemberMapper.findActiveMember(record.getWorkplaceId(), record.getUserId());
        BigDecimal hourlyWage = resolveHourlyWage(record.getWorkplaceId(), staffMember);
        BigDecimal wage = hourlyWage
                .multiply(BigDecimal.valueOf(workedMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        wage = truncateToTenWonUnit(wage);

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
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ApiException("User not found.");
        }
        if (!"OWNER".equalsIgnoreCase(user.getUserType())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only OWNER user type can create workplace.");
        }
    }

    private void validateWorkplaceLocationRequest(CreateWorkplaceRequest request) {
        boolean hasLatitude = request.getLatitude() != null;
        boolean hasLongitude = request.getLongitude() != null;
        if (hasLatitude != hasLongitude) {
            throw new ApiException("latitude and longitude must be provided together.");
        }
        if (Boolean.TRUE.equals(request.getUseLocationRestriction()) && !hasLatitude) {
            throw new ApiException("latitude and longitude are required when useLocationRestriction is true.");
        }
    }

    private BigDecimal resolveHourlyWage(Long workplaceId, WorkplaceMember member) {
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        if (setting != null && setting.getDefaultHourlyWage() != null) {
            return setting.getDefaultHourlyWage();
        }
        if (member != null && member.getHourlyWage() != null) {
            return member.getHourlyWage();
        }
        return BigDecimal.ZERO;
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

    private String normalizeRequestStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"PENDING".equals(normalized)
                && !"APPROVED".equals(normalized)
                && !"REJECTED".equals(normalized)) {
            throw new ApiException("status must be one of PENDING, APPROVED, REJECTED.");
        }
        return normalized;
    }
}
