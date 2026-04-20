package com.api.alba.service.owner;

import com.api.alba.component.WageCalculationHelper;
import com.api.alba.component.WageCalculationHelper.WageCalculationResult;
import com.api.alba.domain.attendance.AttendanceRecord;
import com.api.alba.domain.attendance.AttendanceRequest;
import com.api.alba.domain.auth.User;
import com.api.alba.domain.owner.Workplace;
import com.api.alba.domain.settings.WorkplaceBreakPolicy;
import com.api.alba.domain.settings.WorkplaceSetting;
import com.api.alba.domain.staff.WorkplaceMember;
import com.api.alba.dto.owner.AttendancePushSettingResponse;
import com.api.alba.dto.owner.AttendanceRequestListItemResponse;
import com.api.alba.dto.owner.BreakPoliciesResponse;
import com.api.alba.dto.owner.CreateWorkplaceRequest;
import com.api.alba.dto.owner.DashboardTodayResponse;
import com.api.alba.dto.owner.OwnerDecisionRequest;
import com.api.alba.dto.owner.OwnerWorkplaceMemberResponse;
import com.api.alba.dto.owner.OwnerCreateAttendanceRecordRequest;
import com.api.alba.dto.owner.SaveBreakPoliciesRequest;
import com.api.alba.dto.owner.UpdateLocationRestrictionRequest;
import com.api.alba.dto.owner.UpdateWorkplaceMemberMemoRequest;
import com.api.alba.dto.owner.OwnerDailyAttendanceItemResponse;
import com.api.alba.dto.owner.OwnerMonthlyCalendarItemResponse;
import com.api.alba.dto.staff.EmployeeWageSummary;
import com.api.alba.dto.staff.InviteCodeResponse;
import com.api.alba.dto.staff.StaffMonthlyCalendarItemResponse;
import com.api.alba.exception.ApiException;
import com.api.alba.mapper.attendance.AttendanceRecordMapper;
import com.api.alba.mapper.attendance.AttendanceRequestMapper;
import com.api.alba.mapper.auth.UserMapper;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.api.alba.exception.ExceptionMessages.ACTIVE_WORKPLACE_MEMBER_NOT_FOUND;
import static com.api.alba.exception.ExceptionMessages.ATTENDANCE_RECORD_ALREADY_EXISTS;
import static com.api.alba.exception.ExceptionMessages.ATTENDANCE_RECORD_NOT_FOUND;
import static com.api.alba.exception.ExceptionMessages.CHECK_OUT_MUST_BE_AFTER_CHECK_IN;
import static com.api.alba.exception.ExceptionMessages.INVALID_REQUEST;
import static com.api.alba.exception.ExceptionMessages.ATTENDANCE_REQUEST_NOT_FOUND;
import static com.api.alba.exception.ExceptionMessages.CANNOT_DELETE_OWNER_MEMBER;
import static com.api.alba.exception.ExceptionMessages.INVALID_DATE_RANGE;
import static com.api.alba.exception.ExceptionMessages.LAT_LON_MUST_BE_PROVIDED_TOGETHER;
import static com.api.alba.exception.ExceptionMessages.LAT_LON_REQUIRED_WHEN_USE_LOCATION_RESTRICTION_TRUE;
import static com.api.alba.exception.ExceptionMessages.MEMBER_NOT_FOUND;
import static com.api.alba.exception.ExceptionMessages.ONLY_OWNER_USER_TYPE_CAN_CREATE_WORKPLACE;
import static com.api.alba.exception.ExceptionMessages.ONLY_PENDING_REQUESTS_CAN_BE_PROCESSED;
import static com.api.alba.exception.ExceptionMessages.OWNER_ACCESS_ONLY;
import static com.api.alba.exception.ExceptionMessages.STATUS_MUST_BE_PENDING_APPROVED_REJECTED;
import static com.api.alba.exception.ExceptionMessages.USER_NOT_FOUND;
import static com.api.alba.exception.ExceptionMessages.WORKPLACE_NOT_FOUND;
import static com.api.alba.exception.ExceptionMessages.WORKPLACE_SETTING_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class OwnerService {
    private static final int DEFAULT_ALLOWED_RADIUS_METERS = 100;
    private static final boolean DEFAULT_USE_LOCATION_RESTRICTION = false;
    private static final BigDecimal DEFAULT_WORKPLACE_HOURLY_WAGE = BigDecimal.ZERO;
    private static final String DEFAULT_SALARY_CALC_UNIT = "10MIN";
    private static final String DEFAULT_ROUNDING_POLICY = "NONE";
    private static final Long SUPER_ADMIN_USER_ID = 1L;

    private final WorkplaceMapper workplaceMapper;
    private final WorkplaceMemberMapper workplaceMemberMapper;
    private final WorkplaceSettingMapper workplaceSettingMapper;
    private final WorkplaceBreakPolicyMapper workplaceBreakPolicyMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final AttendanceRequestMapper attendanceRequestMapper;
    private final UserMapper userMapper;
    private final WageCalculationHelper wageCalculationHelper;

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
        workplace.setIsPersonal(false);
        workplaceMapper.insert(workplace);

        WorkplaceMember member = new WorkplaceMember();
        member.setWorkplaceId(workplace.getId());
        member.setUserId(ownerUserId);
        member.setRole("OWNER");
        member.setStatus("ACTIVE");
        member.setHourlyWage(null);
        member.setMemo(null);
        member.setReceiveAttendancePush(true);
        workplaceMemberMapper.insert(member);

        if (!SUPER_ADMIN_USER_ID.equals(ownerUserId)) {
            WorkplaceMember superAdminMember = new WorkplaceMember();
            superAdminMember.setWorkplaceId(workplace.getId());
            superAdminMember.setUserId(SUPER_ADMIN_USER_ID);
            superAdminMember.setRole("OWNER");
            superAdminMember.setStatus("ACTIVE");
            superAdminMember.setHourlyWage(null);
            superAdminMember.setMemo(null);
            superAdminMember.setReceiveAttendancePush(false);
            workplaceMemberMapper.insert(superAdminMember);
        }

        WorkplaceSetting workplaceSetting = new WorkplaceSetting();
        workplaceSetting.setWorkplaceId(workplace.getId());
        workplaceSetting.setLateGraceMinutes(0);
        workplaceSetting.setSalaryCalcUnit(DEFAULT_SALARY_CALC_UNIT);
        workplaceSetting.setRoundingPolicy(DEFAULT_ROUNDING_POLICY);
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
            throw new ApiException(WORKPLACE_NOT_FOUND);
        }
        return new InviteCodeResponse(workplace.getId(), workplace.getInviteCode());
    }

    public DashboardTodayResponse getTodayDashboard(Long ownerUserId, Long workplaceId) {
        ensureOwner(workplaceId, ownerUserId);
        LocalDate today = LocalDate.now();
        int totalStaffCount = workplaceMemberMapper.countActiveStaffByWorkplaceId(workplaceId);
        int checkedIn = attendanceRecordMapper.countTodayCheckedIn(workplaceId, today);
        int working = attendanceRecordMapper.countTodayWorking(workplaceId, today);
        int pendingAttendanceRequestCount = attendanceRequestMapper.countPendingByWorkplaceId(workplaceId);
        return new DashboardTodayResponse(totalStaffCount, checkedIn, working, pendingAttendanceRequestCount);
    }

    public AttendancePushSettingResponse getAttendancePushSetting(Long ownerUserId, Long workplaceId) {
        WorkplaceMember ownerMember = ensureOwner(workplaceId, ownerUserId);
        Workplace workplace = workplaceMapper.findById(workplaceId);
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        if (setting == null) {
            throw new ApiException(WORKPLACE_SETTING_NOT_FOUND);
        }
        boolean receiveAttendancePush = ownerMember != null && Boolean.TRUE.equals(ownerMember.getReceiveAttendancePush());
        return new AttendancePushSettingResponse(
                workplaceId,
                workplace.getName(),
                workplace.getAddress(),
                workplace.getLatitude(),
                workplace.getLongitude(),
                workplace.getUseLocationRestriction(),
                receiveAttendancePush,
                setting.getDefaultHourlyWage(),
                setting.getSalaryCalcUnit(),
                setting.getDefaultCheckInTime(),
                setting.getDefaultCheckOutTime()
        );
    }

    public List<OwnerWorkplaceMemberResponse> getWorkplaceMembers(Long ownerUserId, Long workplaceId) {
        ensureOwner(workplaceId, ownerUserId);
        return workplaceMemberMapper.findActiveStaffMembersByWorkplaceId(workplaceId);
    }

    @Transactional
    public void updateWorkplaceMemberMemo(
            Long ownerUserId,
            Long workplaceId,
            Long memberId,
            UpdateWorkplaceMemberMemoRequest request
    ) {
        ensureOwner(workplaceId, ownerUserId);
        WorkplaceMember member = workplaceMemberMapper.findById(memberId);
        if (member == null || !workplaceId.equals(member.getWorkplaceId())) {
            throw new ApiException(WORKPLACE_NOT_FOUND);
        }
        workplaceMemberMapper.updateMemo(memberId, normalizeMemo(request.getMemo()));
    }

    @Transactional
    public void deleteWorkplaceMember(Long ownerUserId, Long workplaceId, Long memberId) {
        ensureOwner(workplaceId, ownerUserId);
        WorkplaceMember member = workplaceMemberMapper.findById(memberId);
        if (member == null || !workplaceId.equals(member.getWorkplaceId()) || !"ACTIVE".equals(member.getStatus())) {
            throw new ApiException(HttpStatus.NOT_FOUND, MEMBER_NOT_FOUND);
        }
        if ("OWNER".equals(member.getRole())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, CANNOT_DELETE_OWNER_MEMBER);
        }
        workplaceMemberMapper.updateStatus(memberId, "INACTIVE");
    }

    public List<AttendanceRecord> getWorkplaceAttendanceRecords(
            Long ownerUserId,
            Long workplaceId,
            Long userId,
            String userName,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        ensureOwner(workplaceId, ownerUserId);
        if (fromDate.isAfter(toDate)) {
            throw new ApiException(INVALID_DATE_RANGE);
        }
        return attendanceRecordMapper.findWorkplaceRecordsByPeriod(workplaceId, userId, userName, fromDate, toDate);
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
            throw new ApiException(ATTENDANCE_REQUEST_NOT_FOUND);
        }
        if (!"PENDING".equals(attendanceRequest.getStatus())) {
            throw new ApiException(ONLY_PENDING_REQUESTS_CAN_BE_PROCESSED);
        }

        AttendanceRecord record = attendanceRecordMapper.findById(attendanceRequest.getAttendanceRecordId());
        if (record == null) {
            throw new ApiException(ATTENDANCE_RECORD_NOT_FOUND);
        }

        ensureOwner(record.getWorkplaceId(), ownerUserId);

        String status = request.getStatus().toUpperCase();
        if ("APPROVED".equals(status)) {
            applyApprovedRequest(record, attendanceRequest);
        }
        attendanceRequestMapper.updateStatus(requestId, status);
    }

    public List<OwnerDailyAttendanceItemResponse> getDailyAttendance(
            Long ownerUserId,
            Long workplaceId,
            LocalDate workDate
    ) {
        ensureOwner(workplaceId, ownerUserId);
        return attendanceRecordMapper.findDailyAttendanceByWorkDate(workplaceId, workDate);
    }

    public List<OwnerMonthlyCalendarItemResponse> getStaffMonthlyCalendar(
            Long ownerUserId,
            Long workplaceId,
            Long staffUserId,
            String yearMonth
    ) {
        ensureOwner(workplaceId, ownerUserId);
        YearMonth targetMonth = parseYearMonth(yearMonth);
        return attendanceRecordMapper.findOwnerMonthlyCalendarByPeriod(
                workplaceId,
                staffUserId,
                targetMonth.atDay(1),
                targetMonth.atEndOfMonth()
        );
    }

    public List<EmployeeWageSummary> getExpectedWageSummary(
            Long ownerUserId,
            Long workplaceId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        ensureOwner(workplaceId, ownerUserId);
        if (fromDate.isAfter(toDate)) {
            throw new ApiException(INVALID_DATE_RANGE);
        }
        return attendanceRecordMapper.findEmployeeWageSummaryByPeriod(workplaceId, fromDate, toDate);
    }

    @Transactional
    public void updateWorkplaceName(Long ownerUserId, Long workplaceId, String workplaceName) {
        ensureOwner(workplaceId, ownerUserId);
        workplaceMapper.updateName(workplaceId, workplaceName);
    }

    @Transactional
    public void updateLocationRestriction(Long ownerUserId, Long workplaceId, UpdateLocationRestrictionRequest request) {
        ensureOwner(workplaceId, ownerUserId);
        workplaceMapper.updateLocationRestriction(workplaceId, request.getUseLocationRestriction(), request.getAddress(), request.getLatitude(), request.getLongitude());
    }

    @Transactional
    public void updateAttendancePushEnabled(Long ownerUserId, Long workplaceId, Boolean enabled) {
        WorkplaceMember ownerMember = ensureOwner(workplaceId, ownerUserId);
        if (ownerMember != null) {
            workplaceMemberMapper.updateReceiveAttendancePush(ownerMember.getId(), enabled);
        }
    }

    @Transactional
    public void updateHourlyWage(Long ownerUserId, Long workplaceId, BigDecimal hourlyWage) {
        ensureOwner(workplaceId, ownerUserId);
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        if (setting == null) {
            throw new ApiException(WORKPLACE_SETTING_NOT_FOUND);
        }
        workplaceSettingMapper.updateDefaultHourlyWage(workplaceId, hourlyWage);
    }

    @Transactional
    public void updateSalaryCalcUnit(Long ownerUserId, Long workplaceId, String salaryCalcUnit) {
        ensureOwner(workplaceId, ownerUserId);
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        if (setting == null) {
            throw new ApiException(WORKPLACE_SETTING_NOT_FOUND);
        }
        workplaceSettingMapper.updateSalaryCalcUnit(workplaceId, salaryCalcUnit);
    }

    @Transactional
    public void updateDefaultWorkTime(Long ownerUserId, Long workplaceId, java.time.LocalTime defaultCheckInTime, java.time.LocalTime defaultCheckOutTime) {
        ensureOwner(workplaceId, ownerUserId);
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        if (setting == null) {
            throw new ApiException(WORKPLACE_SETTING_NOT_FOUND);
        }
        workplaceSettingMapper.updateDefaultWorkTime(workplaceId, defaultCheckInTime, defaultCheckOutTime);
    }

    @Transactional
    public int recalculateWages(Long ownerUserId, Long workplaceId, YearMonth month) {
        ensureOwner(workplaceId, ownerUserId);
        LocalDate fromDate = month.atDay(1);
        LocalDate toDate = month.atEndOfMonth();
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        List<WorkplaceBreakPolicy> breakPolicies = resolveBreakPolicies(workplaceId, setting);
        List<AttendanceRecord> records = attendanceRecordMapper.findCompletedRecordsByWorkplace(workplaceId, fromDate, toDate);
        for (AttendanceRecord record : records) {
            WorkplaceMember member = workplaceMemberMapper.findActiveMember(workplaceId, record.getUserId());
            BigDecimal hourlyWage = resolveHourlyWage(member, setting);
            int grossWorkedMinutes = calculateWorkedMinutes(record.getCheckInAt(), record.getCheckOutAt());
            WageCalculationResult wageCalculation = wageCalculationHelper.calculate(
                    hourlyWage,
                    grossWorkedMinutes,
                    setting,
                    breakPolicies
            );
            attendanceRecordMapper.updateFinalWage(
                    record.getId(),
                    wageCalculation.finalWage()
            );
        }
        return records.size();
    }

    private void applyApprovedRequest(AttendanceRecord record, AttendanceRequest request) {
        LocalDateTime newCheckIn = request.getRequestedCheckInAt() != null ? request.getRequestedCheckInAt() : record.getCheckInAt();
        LocalDateTime newCheckOut = request.getRequestedCheckOutAt() != null ? request.getRequestedCheckOutAt() : record.getCheckOutAt();

        int grossWorkedMinutes = calculateWorkedMinutes(newCheckIn, newCheckOut);
        WorkplaceMember staffMember = workplaceMemberMapper.findActiveMember(record.getWorkplaceId(), record.getUserId());
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(record.getWorkplaceId());
        List<WorkplaceBreakPolicy> breakPolicies = resolveBreakPolicies(record.getWorkplaceId(), setting);
        BigDecimal hourlyWage = resolveHourlyWage(staffMember, setting);
        WageCalculationResult wageCalculation = wageCalculationHelper.calculate(
                hourlyWage,
                grossWorkedMinutes,
                setting,
                breakPolicies
        );

        String attendanceStatus = newCheckOut == null ? "WORKING" : "COMPLETED";
        attendanceRecordMapper.updateByOwnerDecision(
                record.getId(),
                newCheckIn,
                newCheckOut,
                wageCalculation.workedMinutes(),
                wageCalculation.baseWage(),
                wageCalculation.finalWage(),
                attendanceStatus
        );
    }

    private WorkplaceMember ensureOwner(Long workplaceId, Long userId) {
        User user = userMapper.findById(userId);
        if (user != null && "SUPER_ADMIN".equals(user.getUserType())) {
            return null;
        }
        WorkplaceMember ownerMember = workplaceMemberMapper.findActiveOwnerMember(workplaceId, userId);
        if (ownerMember == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, OWNER_ACCESS_ONLY);
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
            throw new ApiException(USER_NOT_FOUND);
        }
        if (!"OWNER".equalsIgnoreCase(user.getUserType()) && !"SUPER_ADMIN".equalsIgnoreCase(user.getUserType())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ONLY_OWNER_USER_TYPE_CAN_CREATE_WORKPLACE);
        }
    }

    private void validateWorkplaceLocationRequest(CreateWorkplaceRequest request) {
        boolean hasLatitude = request.getLatitude() != null;
        boolean hasLongitude = request.getLongitude() != null;
        if (hasLatitude != hasLongitude) {
            throw new ApiException(LAT_LON_MUST_BE_PROVIDED_TOGETHER);
        }
        if (Boolean.TRUE.equals(request.getUseLocationRestriction()) && !hasLatitude) {
            throw new ApiException(LAT_LON_REQUIRED_WHEN_USE_LOCATION_RESTRICTION_TRUE);
        }
    }

    private BigDecimal resolveHourlyWage(WorkplaceMember member, WorkplaceSetting setting) {
        if (setting != null && setting.getDefaultHourlyWage() != null) {
            return setting.getDefaultHourlyWage();
        }
        if (member != null && member.getHourlyWage() != null) {
            return member.getHourlyWage();
        }
        return BigDecimal.ZERO;
    }

    private int calculateWorkedMinutes(LocalDateTime checkInAt, LocalDateTime checkOutAt) {
        if (checkInAt == null || checkOutAt == null) {
            return 0;
        }
        return (int) Math.max(Duration.between(checkInAt, checkOutAt).toMinutes(), 0);
    }

    private List<WorkplaceBreakPolicy> resolveBreakPolicies(Long workplaceId, WorkplaceSetting setting) {
        if (setting == null || !Boolean.TRUE.equals(setting.getUseBreakPolicy())) {
            return List.of();
        }
        return workplaceBreakPolicyMapper.findAllByWorkplaceId(workplaceId);
    }

    private String normalizeRequestStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"PENDING".equals(normalized)
                && !"APPROVED".equals(normalized)
                && !"REJECTED".equals(normalized)) {
            throw new ApiException(STATUS_MUST_BE_PENDING_APPROVED_REJECTED);
        }
        return normalized;
    }

    private YearMonth parseYearMonth(String yearMonth) {
        try {
            return YearMonth.parse(yearMonth);
        } catch (DateTimeParseException e) {
            throw new ApiException(INVALID_REQUEST);
        }
    }

    public BreakPoliciesResponse getBreakPolicies(Long ownerUserId, Long workplaceId) {
        ensureOwner(workplaceId, ownerUserId);
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        if (setting == null) {
            throw new ApiException(WORKPLACE_SETTING_NOT_FOUND);
        }
        List<WorkplaceBreakPolicy> policies = workplaceBreakPolicyMapper.findAllByWorkplaceId(workplaceId);
        List<BreakPoliciesResponse.PolicyItem> items = new ArrayList<>();
        for (WorkplaceBreakPolicy p : policies) {
            items.add(new BreakPoliciesResponse.PolicyItem(
                    p.getId(), p.getName(), p.getBreakType(),
                    p.getMinWorkMinutes(), p.getBreakMinutes(),
                    p.getIsPaid(), p.getIsActive()
            ));
        }
        return new BreakPoliciesResponse(Boolean.TRUE.equals(setting.getUseBreakPolicy()), items);
    }

    @Transactional
    public List<AttendanceRecord> createAttendanceRecord(Long ownerUserId, Long workplaceId, OwnerCreateAttendanceRecordRequest request) {
        ensureOwner(workplaceId, ownerUserId);

        if (request.getCheckOutAt() != null && !request.getCheckOutAt().isAfter(request.getCheckInAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, CHECK_OUT_MUST_BE_AFTER_CHECK_IN);
        }

        List<WorkplaceMember> targets;
        if (request.getStaffUserId() == 0) {
            targets = workplaceMemberMapper.findAllActiveStaffMembers(workplaceId);
            if (targets.isEmpty()) {
                throw new ApiException(HttpStatus.NOT_FOUND, ACTIVE_WORKPLACE_MEMBER_NOT_FOUND);
            }
            for (WorkplaceMember member : targets) {
                AttendanceRecord existing = attendanceRecordMapper.findByWorkplaceUserAndDate(
                        workplaceId, member.getUserId(), request.getWorkDate()
                );
                if (existing != null) {
                    User user = userMapper.findById(member.getUserId());
                    String name = user != null ? user.getName() : String.valueOf(member.getUserId());
                    throw new ApiException(HttpStatus.CONFLICT, name + "님은 " + ATTENDANCE_RECORD_ALREADY_EXISTS);
                }
            }
        } else {
            WorkplaceMember staffMember = workplaceMemberMapper.findActiveMember(workplaceId, request.getStaffUserId());
            if (staffMember == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, ACTIVE_WORKPLACE_MEMBER_NOT_FOUND);
            }
            AttendanceRecord existing = attendanceRecordMapper.findByWorkplaceUserAndDate(
                    workplaceId, request.getStaffUserId(), request.getWorkDate()
            );
            if (existing != null) {
                User user = userMapper.findById(request.getStaffUserId());
                String name = user != null ? user.getName() : String.valueOf(request.getStaffUserId());
                throw new ApiException(HttpStatus.CONFLICT, name + "님은 " + ATTENDANCE_RECORD_ALREADY_EXISTS);
            }
            targets = List.of(staffMember);
        }

        WorkplaceSetting setting = request.getCheckOutAt() != null
                ? workplaceSettingMapper.findByWorkplaceId(workplaceId)
                : null;
        List<WorkplaceBreakPolicy> breakPolicies = setting != null
                ? resolveBreakPolicies(workplaceId, setting)
                : null;

        List<AttendanceRecord> results = new ArrayList<>();
        for (WorkplaceMember member : targets) {
            AttendanceRecord record = new AttendanceRecord();
            record.setWorkplaceId(workplaceId);
            record.setUserId(member.getUserId());
            record.setWorkDate(request.getWorkDate());
            record.setCheckInAt(request.getCheckInAt());
            record.setNote(request.getNote());
            record.setWorkedMinutes(0);
            record.setBaseWage(BigDecimal.ZERO);
            record.setFinalWage(BigDecimal.ZERO);
            record.setStatus("WORKING");
            attendanceRecordMapper.insert(record);

            if (request.getCheckOutAt() != null) {
                BigDecimal hourlyWage = resolveHourlyWage(member, setting);
                int grossWorkedMinutes = calculateWorkedMinutes(request.getCheckInAt(), request.getCheckOutAt());
                WageCalculationResult wageCalculation = wageCalculationHelper.calculate(
                        hourlyWage, grossWorkedMinutes, setting, breakPolicies
                );
                attendanceRecordMapper.updateCheckOut(
                        record.getId(),
                        request.getCheckOutAt(),
                        wageCalculation.workedMinutes(),
                        wageCalculation.baseWage(),
                        wageCalculation.finalWage(),
                        "COMPLETED"
                );
            }

            results.add(attendanceRecordMapper.findByWorkplaceUserAndDate(workplaceId, member.getUserId(), request.getWorkDate()));
        }

        return results;
    }

    @Transactional
    public void saveBreakPolicies(Long ownerUserId, Long workplaceId, SaveBreakPoliciesRequest request) {
        ensureOwner(workplaceId, ownerUserId);
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        if (setting == null) {
            throw new ApiException(WORKPLACE_SETTING_NOT_FOUND);
        }
        workplaceSettingMapper.updateUseBreakPolicy(workplaceId, request.getUseBreakPolicy());
        workplaceBreakPolicyMapper.deleteByWorkplaceId(workplaceId);
        if (request.getPolicies() != null) {
            for (SaveBreakPoliciesRequest.PolicyItem item : request.getPolicies()) {
                WorkplaceBreakPolicy policy = new WorkplaceBreakPolicy();
                policy.setWorkplaceId(workplaceId);
                policy.setName(item.getName());
                policy.setBreakType(item.getBreakType());
                policy.setMinWorkMinutes(item.getMinWorkMinutes());
                policy.setBreakMinutes(item.getBreakMinutes());
                policy.setIsPaid(item.getIsPaid());
                policy.setIsActive(true);
                workplaceBreakPolicyMapper.insert(policy);
            }
        }
    }

    private String normalizeMemo(String memo) {
        if (memo == null) {
            return null;
        }
        String trimmed = memo.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
