package com.api.alba.service.owner;

import com.api.alba.component.InsuranceCalculationHelper;
import com.api.alba.component.WageCalculationHelper;
import com.api.alba.component.WageCalculationHelper.WageCalculationResult;
import com.api.alba.domain.attendance.AttendanceRecord;
import com.api.alba.domain.attendance.AttendanceRequest;
import com.api.alba.domain.auth.User;
import com.api.alba.domain.owner.EmployeeInsuranceSetting;
import com.api.alba.domain.owner.Payslip;
import com.api.alba.domain.owner.PayslipDeduction;
import com.api.alba.domain.owner.Workplace;
import com.api.alba.domain.settings.WorkplaceBreakPolicy;
import com.api.alba.domain.settings.WorkplaceSetting;
import com.api.alba.domain.staff.WorkplaceMember;
import com.api.alba.domain.staff.WorkplaceMemberSchedule;
import com.api.alba.dto.owner.*;
import com.api.alba.dto.push.StaffReminderTarget;
import com.api.alba.dto.staff.EmployeeWageSummary;
import com.api.alba.dto.staff.InviteCodeResponse;
import com.api.alba.exception.ApiException;
import com.api.alba.firebase.FcmDto;
import com.api.alba.firebase.FcmService;
import com.api.alba.firebase.ProjectId;
import com.api.alba.mapper.attendance.AttendanceRecordMapper;
import com.api.alba.mapper.attendance.AttendanceRequestMapper;
import com.api.alba.mapper.auth.UserMapper;
import com.api.alba.mapper.owner.EmployeeInsuranceSettingMapper;
import com.api.alba.mapper.owner.PayslipDeductionMapper;
import com.api.alba.mapper.owner.PayslipMapper;
import com.api.alba.mapper.owner.WorkplaceMapper;
import com.api.alba.mapper.push.PushTokenMapper;
import com.api.alba.mapper.settings.WorkplaceBreakPolicyMapper;
import com.api.alba.mapper.settings.WorkplaceSettingMapper;
import com.api.alba.mapper.staff.WorkplaceMemberMapper;
import com.api.alba.mapper.staff.WorkplaceMemberScheduleMapper;
import com.api.alba.dto.attendance.QrTokenResponse;
import com.api.alba.security.JwtTokenProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import static com.api.alba.exception.ExceptionMessages.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerService {
    private static final int DEFAULT_ALLOWED_RADIUS_METERS = 150;
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
    private final InsuranceCalculationHelper insuranceCalculationHelper;
    private final PayslipMapper payslipMapper;
    private final PayslipDeductionMapper payslipDeductionMapper;
    private final EmployeeInsuranceSettingMapper employeeInsuranceSettingMapper;
    private final ObjectMapper objectMapper;
    private final PushTokenMapper pushTokenMapper;
    private final FcmService fcmService;
    private final WorkplaceMemberScheduleMapper workplaceMemberScheduleMapper;
    private final JwtTokenProvider jwtTokenProvider;

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
        workplace.setUseQrAttendance(false);
        workplace.setIsPersonal(false);
        workplaceMapper.insert(workplace);

        WorkplaceMember member = new WorkplaceMember();
        member.setWorkplaceId(workplace.getId());
        member.setUserId(ownerUserId);
        member.setRole("OWNER");
        member.setWageType("HOURLY");
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
            superAdminMember.setWageType("HOURLY");
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
        ensureOwnerOrManager(workplaceId, ownerUserId);
        Workplace workplace = workplaceMapper.findById(workplaceId);
        if (workplace == null) {
            throw new ApiException(WORKPLACE_NOT_FOUND);
        }
        return new InviteCodeResponse(workplace.getId(), workplace.getInviteCode());
    }

    public DashboardTodayResponse getTodayDashboard(Long ownerUserId, Long workplaceId) {
        ensureOwnerOrManager(workplaceId, ownerUserId);
        LocalDate today = LocalDate.now();
        int totalStaffCount = workplaceMemberMapper.countActiveStaffByWorkplaceId(workplaceId);
        int checkedIn = attendanceRecordMapper.countTodayCheckedIn(workplaceId, today);
        int working = attendanceRecordMapper.countTodayWorking(workplaceId, today);
        int pendingAttendanceRequestCount = attendanceRequestMapper.countPendingByWorkplaceId(workplaceId);
        return new DashboardTodayResponse(totalStaffCount, checkedIn, working, pendingAttendanceRequestCount);
    }

    public AttendancePushSettingResponse getAttendancePushSetting(Long ownerUserId, Long workplaceId) {
        WorkplaceMember member = workplaceMemberMapper.findActiveMember(workplaceId, ownerUserId);
        if (member == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, OWNER_ACCESS_ONLY);
        }
        Workplace workplace = workplaceMapper.findById(workplaceId);
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        if (setting == null) {
            throw new ApiException(WORKPLACE_SETTING_NOT_FOUND);
        }
        boolean receiveAttendancePush = Boolean.TRUE.equals(member.getReceiveAttendancePush());
        return new AttendancePushSettingResponse(
                workplaceId,
                workplace.getName(),
                workplace.getAddress(),
                workplace.getLatitude(),
                workplace.getLongitude(),
                workplace.getUseLocationRestriction(),
                workplace.getUseQrAttendance(),
                workplace.getQrNoTimeLimit(),
                workplace.getQrPin(),
                receiveAttendancePush,
                setting.getDefaultHourlyWage(),
                setting.getSalaryCalcUnit(),
                setting.getDefaultCheckInTime(),
                setting.getDefaultCheckOutTime(),
                Boolean.TRUE.equals(setting.getUseWeeklyHolidayPay())
        );
    }

    public List<OwnerWorkplaceMemberResponse> getWorkplaceMembers(Long ownerUserId, Long workplaceId) {
        ensureOwnerOrManager(workplaceId, ownerUserId);
        List<OwnerWorkplaceMemberResponse> members = workplaceMemberMapper.findActiveStaffMembersByWorkplaceId(workplaceId);

        Map<Long, List<MemberScheduleItemResponse>> scheduleMap = workplaceMemberScheduleMapper.findAllByWorkplaceId(workplaceId)
                .stream()
                .collect(Collectors.groupingBy(
                        WorkplaceMemberSchedule::getUserId,
                        Collectors.mapping(s -> new MemberScheduleItemResponse(s.getDayOfWeek(), s.getScheduledCheckInTime(), s.getScheduledCheckOutTime()), Collectors.toList())
                ));
        members.forEach(m -> m.setScheduleDays(scheduleMap.getOrDefault(m.getUserId(), List.of())));
        return members;
    }

    @Transactional
    public void updateMemberWage(Long ownerUserId, Long workplaceId, Long memberId, UpdateMemberWageRequest request) {
        ensureOwnerOrManager(workplaceId, ownerUserId);
        WorkplaceMember member = workplaceMemberMapper.findById(memberId);
        if (member == null || !workplaceId.equals(member.getWorkplaceId())) {
            throw new ApiException(WORKPLACE_NOT_FOUND);
        }
        String wageType = request.getWageType().toUpperCase();
        if (!"HOURLY".equals(wageType) && !"MONTHLY".equals(wageType) && !"DAILY".equals(wageType)) {
            throw new ApiException(INVALID_WAGE_TYPE);
        }
        if ("HOURLY".equals(wageType) && request.getHourlyWage() == null) {
            throw new ApiException(HOURLY_WAGE_REQUIRED);
        }
        if ("MONTHLY".equals(wageType) && request.getMonthlyWage() == null) {
            throw new ApiException(MONTHLY_WAGE_REQUIRED);
        }
        if ("DAILY".equals(wageType) && request.getDailyWage() == null) {
            throw new ApiException(DAILY_WAGE_REQUIRED);
        }
        BigDecimal hourlyWage = "HOURLY".equals(wageType) ? request.getHourlyWage() : null;
        BigDecimal monthlyWage = "MONTHLY".equals(wageType) ? request.getMonthlyWage() : null;
        BigDecimal dailyWage = "DAILY".equals(wageType) ? request.getDailyWage() : null;
        workplaceMemberMapper.updateWage(memberId, wageType, hourlyWage, monthlyWage, dailyWage);
        if ("MONTHLY".equals(wageType) || "DAILY".equals(wageType)) {
            attendanceRecordMapper.clearWagesByMember(workplaceId, member.getUserId());
        }
    }

    @Transactional
    public void updateMemberBreakMinutes(Long ownerUserId, Long workplaceId, Long memberId, Integer breakMinutes) {
        ensureOwnerOrManager(workplaceId, ownerUserId);
        WorkplaceMember member = workplaceMemberMapper.findById(memberId);
        if (member == null || !workplaceId.equals(member.getWorkplaceId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, MEMBER_NOT_FOUND);
        }
        workplaceMemberMapper.updateBreakMinutes(memberId, breakMinutes);
    }

    @Transactional
    public void updateMemberUseWeeklyHolidayPay(Long ownerUserId, Long workplaceId, Long memberId, Boolean useWeeklyHolidayPay) {
        ensureOwner(workplaceId, ownerUserId);
        WorkplaceMember member = workplaceMemberMapper.findById(memberId);
        if (member == null || !workplaceId.equals(member.getWorkplaceId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, MEMBER_NOT_FOUND);
        }
        workplaceMemberMapper.updateUseWeeklyHolidayPay(memberId, useWeeklyHolidayPay);
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
    public void updateMemberRole(Long ownerUserId, Long workplaceId, Long memberId, String role) {
        ensureOwner(workplaceId, ownerUserId);
        String normalizedRole = role.toUpperCase();
        if (!"STAFF".equals(normalizedRole) && !"MANAGER".equals(normalizedRole)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_MEMBER_ROLE);
        }
        WorkplaceMember member = workplaceMemberMapper.findById(memberId);
        if (member == null || !workplaceId.equals(member.getWorkplaceId()) || !"ACTIVE".equals(member.getStatus())) {
            throw new ApiException(HttpStatus.NOT_FOUND, MEMBER_NOT_FOUND);
        }
        if ("OWNER".equals(member.getRole())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, CANNOT_CHANGE_OWNER_ROLE);
        }
        workplaceMemberMapper.updateRole(memberId, normalizedRole);
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

    public List<MemberScheduleItemResponse> getMemberSchedules(Long ownerUserId, Long workplaceId, Long memberId) {
        ensureOwnerOrManager(workplaceId, ownerUserId);
        WorkplaceMember member = workplaceMemberMapper.findById(memberId);
        if (member == null || !workplaceId.equals(member.getWorkplaceId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, MEMBER_NOT_FOUND);
        }
        return workplaceMemberScheduleMapper.findByWorkplaceAndUser(workplaceId, member.getUserId()).stream()
                .map(s -> new MemberScheduleItemResponse(s.getDayOfWeek(), s.getScheduledCheckInTime(), s.getScheduledCheckOutTime()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<MemberScheduleItemResponse> saveMemberSchedules(Long ownerUserId, Long workplaceId, Long memberId, SaveMemberScheduleRequest request) {
        ensureOwner(workplaceId, ownerUserId);
        WorkplaceMember member = workplaceMemberMapper.findById(memberId);
        if (member == null || !workplaceId.equals(member.getWorkplaceId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, MEMBER_NOT_FOUND);
        }
        workplaceMemberScheduleMapper.deleteByWorkplaceAndUser(workplaceId, member.getUserId());
        if (request.getSchedules() != null && !request.getSchedules().isEmpty()) {
            List<WorkplaceMemberSchedule> schedules = request.getSchedules().stream()
                    .map(item -> {
                        WorkplaceMemberSchedule s = new WorkplaceMemberSchedule();
                        s.setWorkplaceId(workplaceId);
                        s.setUserId(member.getUserId());
                        s.setDayOfWeek(item.getDayOfWeek());
                        s.setScheduledCheckInTime(item.getScheduledCheckInTime());
                        s.setScheduledCheckOutTime(item.getScheduledCheckOutTime());
                        return s;
                    })
                    .collect(Collectors.toList());
            workplaceMemberScheduleMapper.insertAll(schedules);
        }
        return workplaceMemberScheduleMapper.findByWorkplaceAndUser(workplaceId, member.getUserId()).stream()
                .map(s -> new MemberScheduleItemResponse(s.getDayOfWeek(), s.getScheduledCheckInTime(), s.getScheduledCheckOutTime()))
                .collect(Collectors.toList());
    }

    public List<AttendanceRecord> getWorkplaceAttendanceRecords(
            Long ownerUserId,
            Long workplaceId,
            Long userId,
            String userName,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        ensureOwnerOrManager(workplaceId, ownerUserId);
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
        ensureOwnerOrManager(workplaceId, ownerUserId);
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

        ensureOwnerOrManager(record.getWorkplaceId(), ownerUserId);

        String status = request.getStatus().toUpperCase();
        if ("APPROVED".equals(status)) {
            applyApprovedRequest(record, attendanceRequest);
        } else if ("REJECTED".equals(status) && "NEW_RECORD".equals(attendanceRequest.getType())) {
            // NEW_RECORD 요청 반려 시 요청 제출 시 생성된 빈 레코드 삭제
            attendanceRecordMapper.deleteById(record.getId());
        }
        attendanceRequestMapper.updateStatus(requestId, status);
    }

    @Transactional
    public void deleteAttendanceRecord(Long ownerUserId, Long workplaceId, Long recordId) {
        ensureOwnerOrManager(workplaceId, ownerUserId);
        AttendanceRecord record = attendanceRecordMapper.findById(recordId);
        if (record == null) {
            throw new ApiException(ATTENDANCE_RECORD_NOT_FOUND);
        }
        attendanceRecordMapper.deleteById(recordId);
    }

    @Transactional
    public AttendanceRecord updateAttendanceRecord(Long ownerUserId, Long workplaceId, Long recordId, OwnerUpdateAttendanceRecordRequest request) {
        ensureOwnerOrManager(workplaceId, ownerUserId);

        if (request.getCheckOutAt() != null && !request.getCheckOutAt().isAfter(request.getCheckInAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, CHECK_OUT_MUST_BE_AFTER_CHECK_IN);
        }

        AttendanceRecord record = attendanceRecordMapper.findById(recordId);
        if (record == null) {
            throw new ApiException(ATTENDANCE_RECORD_NOT_FOUND);
        }

        LocalDateTime newCheckIn = request.getCheckInAt();
        LocalDateTime newCheckOut = request.getCheckOutAt();

        WorkplaceMember staffMember = workplaceMemberMapper.findActiveMember(workplaceId, record.getUserId());
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        List<WorkplaceBreakPolicy> breakPolicies = resolveBreakPolicies(workplaceId, setting);
        int grossWorkedMinutes = newCheckOut != null ? calculateWorkedMinutes(newCheckIn, newCheckOut) : 0;
        String memberWageType = staffMember != null ? staffMember.getWageType() : null;
        int resolvedWorkedMinutes;
        BigDecimal resolvedBaseWage;
        BigDecimal resolvedFinalWage;
        if ("DAILY".equals(memberWageType) && newCheckOut != null) {
            BigDecimal dw = staffMember.getDailyWage() != null ? staffMember.getDailyWage() : BigDecimal.ZERO;
            resolvedWorkedMinutes = wageCalculationHelper.calculatePayableWorkedMinutes(grossWorkedMinutes, setting, breakPolicies, staffMember.getBreakMinutes() != null ? staffMember.getBreakMinutes() : 0);
            resolvedBaseWage = dw;
            resolvedFinalWage = dw;
        } else {
            BigDecimal hourlyWage = "MONTHLY".equals(memberWageType) ? BigDecimal.ZERO : resolveHourlyWage(staffMember, setting);
            WageCalculationResult wageCalculation = wageCalculationHelper.calculate(hourlyWage, grossWorkedMinutes, setting, breakPolicies, staffMember != null && staffMember.getBreakMinutes() != null ? staffMember.getBreakMinutes() : 0);
            resolvedWorkedMinutes = wageCalculation.workedMinutes();
            resolvedBaseWage = wageCalculation.baseWage();
            resolvedFinalWage = wageCalculation.finalWage();
        }

        String status = newCheckOut == null ? "WORKING" : resolveCheckOutStatus(newCheckIn, setting);
        attendanceRecordMapper.updateByOwnerDecision(
                recordId,
                newCheckIn,
                newCheckOut,
                resolvedWorkedMinutes,
                resolvedBaseWage,
                resolvedFinalWage,
                status,
                request.getNote()
        );

        return attendanceRecordMapper.findById(recordId);
    }

    public List<OwnerDailyAttendanceItemResponse> getDailyAttendance(
            Long ownerUserId,
            Long workplaceId,
            LocalDate workDate
    ) {
        ensureOwnerOrManager(workplaceId, ownerUserId);
        return attendanceRecordMapper.findDailyAttendanceByWorkDate(workplaceId, workDate);
    }

    public List<OwnerMonthlyCalendarItemResponse> getStaffMonthlyCalendar(
            Long ownerUserId,
            Long workplaceId,
            Long staffUserId,
            String yearMonth
    ) {
        ensureOwnerOrManager(workplaceId, ownerUserId);
        YearMonth targetMonth = parseYearMonth(yearMonth);
        return attendanceRecordMapper.findOwnerMonthlyCalendarByPeriod(
                workplaceId,
                staffUserId,
                targetMonth.atDay(1),
                targetMonth.atEndOfMonth()
        );
    }

    public PayslipResponse getPayslip(Long ownerUserId, Long workplaceId, Long memberId, LocalDate startDate, LocalDate endDate) {
        ensureOwner(workplaceId, ownerUserId);
        WorkplaceMember member = workplaceMemberMapper.findMember(workplaceId, memberId);
        if (member == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, MEMBER_NOT_FOUND);
        }
        User staff = userMapper.findById(member.getUserId());
        String staffName = staff != null ? staff.getName() : "";

        List<AttendanceRecord> records = attendanceRecordMapper.findMyRecordsByPeriod(
                workplaceId, member.getUserId(), startDate, endDate
        );
        Collections.reverse(records);

        List<PayslipDailyItemResponse> dailyList = records.stream()
                .map(r -> new PayslipDailyItemResponse(
                        r.getWorkDate(),
                        r.getCheckInAt(),
                        r.getCheckOutAt(),
                        r.getWorkedMinutes() != null ? r.getWorkedMinutes() : 0,
                        r.getFinalWage() != null ? r.getFinalWage() : BigDecimal.ZERO,
                        r.getStatus(),
                        r.getWageType(),
                        r.getMonthlyWage(),
                        r.getDailyWage()
                ))
                .collect(Collectors.toList());

        int totalWorkedMinutes = dailyList.stream().mapToInt(PayslipDailyItemResponse::getWorkedMinutes).sum();
        BigDecimal totalWage = dailyList.stream()
                .map(PayslipDailyItemResponse::getFinalWage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PayslipResponse(staffName, startDate, endDate, dailyList, dailyList.size(), totalWorkedMinutes, totalWage);
    }

    @Transactional
    public IssuePayslipResponse issuePayslips(Long ownerUserId, Long workplaceId, IssuePayslipRequest request) {
        ensureOwner(workplaceId, ownerUserId);
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        List<WorkplaceBreakPolicy> breakPolicies = resolveBreakPolicies(workplaceId, setting);

        int issuedCount = 0;
        for (Long userId : request.getUserIds()) {
            WorkplaceMember member = workplaceMemberMapper.findActiveMember(workplaceId, userId);
            if (member == null) continue;

            boolean isMonthly = "MONTHLY".equals(member.getWageType());
            boolean isDaily = "DAILY".equals(member.getWageType());
            BigDecimal hourlyWage = (isMonthly || isDaily) ? BigDecimal.ZERO : resolveHourlyWage(member, setting);
            BigDecimal dailyWageAmount = isDaily ? (member.getDailyWage() != null ? member.getDailyWage() : BigDecimal.ZERO) : BigDecimal.ZERO;
            List<PayslipRecordItem> records = buildRecords(workplaceId, member, setting, breakPolicies, hourlyWage, request.getFromDate(), request.getToDate());
            int totalWorkedMinutes = records.stream().mapToInt(PayslipRecordItem::getWorkedMinutes).sum();

            BigDecimal baseWage;
            BigDecimal weeklyHolidayPay = BigDecimal.ZERO;
            if (isMonthly) {
                baseWage = member.getMonthlyWage() != null ? member.getMonthlyWage() : BigDecimal.ZERO;
            } else if (isDaily) {
                baseWage = dailyWageAmount.multiply(BigDecimal.valueOf(records.size()));
                boolean applyWeeklyHolidayPay = member.getUseWeeklyHolidayPay() != null
                        ? member.getUseWeeklyHolidayPay()
                        : Boolean.TRUE.equals(setting.getUseWeeklyHolidayPay());
                if (applyWeeklyHolidayPay) {
                    BigDecimal equivalentHourly = dailyWageAmount.divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP);
                    weeklyHolidayPay = calculateWeeklyHolidayPay(records, equivalentHourly);
                }
            } else {
                baseWage = records.stream().map(PayslipRecordItem::getDailyWage).reduce(BigDecimal.ZERO, BigDecimal::add);
                boolean applyWeeklyHolidayPay = member.getUseWeeklyHolidayPay() != null
                        ? member.getUseWeeklyHolidayPay()
                        : Boolean.TRUE.equals(setting.getUseWeeklyHolidayPay());
                if (applyWeeklyHolidayPay) {
                    weeklyHolidayPay = calculateWeeklyHolidayPay(records, hourlyWage);
                }
            }

            Payslip payslip = new Payslip();
            payslip.setWorkplaceId(workplaceId);
            payslip.setUserId(userId);
            payslip.setFromDate(request.getFromDate());
            payslip.setToDate(request.getToDate());
            payslip.setWageType(isMonthly ? "MONTHLY" : isDaily ? "DAILY" : "HOURLY");
            payslip.setHourlyWage((isMonthly || isDaily) ? BigDecimal.ZERO : hourlyWage);
            payslip.setMonthlyWage(isMonthly ? baseWage : BigDecimal.ZERO);
            payslip.setDailyWage(isDaily ? dailyWageAmount : BigDecimal.ZERO);
            payslip.setWorkedDays(records.size());
            payslip.setWorkedMinutes(totalWorkedMinutes);
            payslip.setBaseWage(baseWage);
            payslip.setWeeklyHolidayPay(weeklyHolidayPay);
            payslip.setBonusAmount(BigDecimal.ZERO);
            payslip.setDeductionAmount(BigDecimal.ZERO);
            payslip.setTotalWage(baseWage.add(weeklyHolidayPay));
            payslip.setDailySnapshot(serializeSnapshot(records));
            payslip.setStatus("ISSUED");
            payslipMapper.insert(payslip);

            // 4대보험 자동 공제 계산
            autoInsertInsuranceDeductions(payslip, member, baseWage, weeklyHolidayPay);

            issuedCount++;
        }
        return new IssuePayslipResponse(issuedCount);
    }

    private void autoInsertInsuranceDeductions(Payslip payslip, WorkplaceMember member,
                                                BigDecimal baseWage, BigDecimal weeklyHolidayPay) {
        EmployeeInsuranceSetting setting = employeeInsuranceSettingMapper.findByWorkplaceMemberId(member.getId());
        if (setting == null) return;

        // 기준일: 급여 기간 종료일 (귀속월 말일 기준)
        LocalDate baseDate = payslip.getToDate();
        // 총 지급액 = 기본급 + 주휴수당 (추가 지급액 제외 - 발행 시점에는 0)
        BigDecimal totalPay = baseWage.add(weeklyHolidayPay != null ? weeklyHolidayPay : BigDecimal.ZERO);

        List<PayslipDeduction> deductions = insuranceCalculationHelper.calculate(setting, totalPay, baseDate);
        if (deductions.isEmpty()) return;

        BigDecimal totalDeductionAmount = BigDecimal.ZERO;
        for (PayslipDeduction d : deductions) {
            d.setPayslipId(payslip.getId());
            payslipDeductionMapper.insert(d);
            totalDeductionAmount = totalDeductionAmount.add(d.getAmount());
        }

        BigDecimal newTotal = payslip.getTotalWage().subtract(totalDeductionAmount);
        payslipMapper.updateDeductionSnapshot(payslip.getId(), totalDeductionAmount, newTotal);
        payslip.setDeductionAmount(totalDeductionAmount);
        payslip.setTotalWage(newTotal);
    }

    public List<PayslipListItemResponse> getPayslips(Long ownerUserId, Long workplaceId, String yearMonth) {
        ensureOwner(workplaceId, ownerUserId);
        YearMonth ym = YearMonth.parse(yearMonth);
        return payslipMapper.findByWorkplaceId(workplaceId, ym.atDay(1), ym.atEndOfMonth()).stream()
                .map(p -> {
                    BigDecimal whp = p.getWeeklyHolidayPay() != null ? p.getWeeklyHolidayPay() : BigDecimal.ZERO;
                    return new PayslipListItemResponse(
                            p.getId(), p.getUserId(), p.getUserName(), p.getProfileColor(),
                            p.getFromDate(), p.getToDate(), p.getCreatedAt().toLocalDate(),
                            p.getWorkedDays(), p.getWorkedMinutes(), p.getWageType(), p.getHourlyWage(), p.getMonthlyWage(),
                            p.getDailyWage() != null ? p.getDailyWage() : BigDecimal.ZERO,
                            p.getBaseWage(), whp, p.getBonusAmount(), p.getDeductionAmount(), p.getTotalWage(),
                            p.getStatus()
                    );
                })
                .collect(Collectors.toList());
    }

    public PayslipDetailResponse getPayslipDetail(Long ownerUserId, Long workplaceId, Long payslipId) {
        ensureOwner(workplaceId, ownerUserId);
        Payslip payslip = findPayslipOrThrow(workplaceId, payslipId);
        return toOwnerDetailResponse(payslip);
    }

    @Transactional
    public PayslipDetailResponse updatePayslip(Long ownerUserId, Long workplaceId, Long payslipId, UpdatePayslipRequest request) {
        ensureOwner(workplaceId, ownerUserId);
        Payslip payslip = findPayslipOrThrow(workplaceId, payslipId);
        if ("CANCELLED".equals(payslip.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, PAYSLIP_ALREADY_CANCELLED);
        }
        BigDecimal weeklyHolidayPay = payslip.getWeeklyHolidayPay() != null ? payslip.getWeeklyHolidayPay() : BigDecimal.ZERO;
        BigDecimal totalWage = payslip.getBaseWage()
                .add(weeklyHolidayPay)
                .add(request.getBonusAmount())
                .subtract(payslip.getDeductionAmount());
        payslipMapper.updateBonus(payslipId, request.getBonusAmount(), request.getBonusNote(), totalWage);
        return toOwnerDetailResponse(payslipMapper.findById(payslipId));
    }

    @Transactional
    public PayslipDetailResponse savePayslipDeductions(Long ownerUserId, Long workplaceId, Long payslipId, SavePayslipDeductionsRequest request) {
        ensureOwner(workplaceId, ownerUserId);
        Payslip payslip = findPayslipOrThrow(workplaceId, payslipId);
        if ("CANCELLED".equals(payslip.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, PAYSLIP_ALREADY_CANCELLED);
        }
        payslipDeductionMapper.deleteByPayslipId(payslipId);
        for (SavePayslipDeductionsRequest.DeductionItem item : request.getDeductions()) {
            PayslipDeduction deduction = new PayslipDeduction();
            deduction.setPayslipId(payslipId);
            deduction.setDeductionType(item.getDeductionType());
            deduction.setName(item.getName());
            deduction.setAmount(item.getAmount());
            deduction.setNote(item.getNote());
            deduction.setDisplayOrder(item.getDisplayOrder());
            payslipDeductionMapper.insert(deduction);
        }
        BigDecimal totalDeduction = request.getDeductions().stream()
                .map(SavePayslipDeductionsRequest.DeductionItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal whp = payslip.getWeeklyHolidayPay() != null ? payslip.getWeeklyHolidayPay() : BigDecimal.ZERO;
        BigDecimal totalWage = payslip.getBaseWage()
                .add(whp)
                .add(payslip.getBonusAmount())
                .subtract(totalDeduction);
        payslipMapper.updateDeductionSnapshot(payslipId, totalDeduction, totalWage);
        return toOwnerDetailResponse(payslipMapper.findById(payslipId));
    }

    public EmployeeInsuranceSettingResponse getEmployeeInsuranceSetting(Long ownerUserId, Long workplaceId, Long memberId) {
        ensureOwner(workplaceId, ownerUserId);
        WorkplaceMember member = workplaceMemberMapper.findById(memberId);
        if (member == null || !member.getWorkplaceId().equals(workplaceId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, MEMBER_NOT_FOUND);
        }
        EmployeeInsuranceSetting setting = employeeInsuranceSettingMapper.findByWorkplaceMemberId(memberId);
        if (setting == null) {
            setting = defaultInsuranceSetting(memberId);
        }
        return new EmployeeInsuranceSettingResponse(setting);
    }

    @Transactional
    public EmployeeInsuranceSettingResponse updateEmployeeInsuranceSetting(Long ownerUserId, Long workplaceId, Long memberId,
                                                                            UpdateEmployeeInsuranceSettingRequest request) {
        ensureOwner(workplaceId, ownerUserId);
        WorkplaceMember member = workplaceMemberMapper.findById(memberId);
        if (member == null || !member.getWorkplaceId().equals(workplaceId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, MEMBER_NOT_FOUND);
        }
        EmployeeInsuranceSetting existing = employeeInsuranceSettingMapper.findByWorkplaceMemberId(memberId);
        EmployeeInsuranceSetting setting = toInsuranceSetting(memberId, request);
        if (existing == null) {
            employeeInsuranceSettingMapper.insert(setting);
        } else {
            employeeInsuranceSettingMapper.update(setting);
        }
        return new EmployeeInsuranceSettingResponse(setting);
    }

    private EmployeeInsuranceSetting defaultInsuranceSetting(Long workplaceMemberId) {
        EmployeeInsuranceSetting setting = new EmployeeInsuranceSetting();
        setting.setWorkplaceMemberId(workplaceMemberId);
        setting.setUseNationalPension(false);
        setting.setUseHealthInsurance(false);
        setting.setUseLongTermCare(false);
        setting.setUseEmploymentInsurance(false);
        setting.setUseIncomeTax(false);
        setting.setTaxFreeAmount(BigDecimal.ZERO);
        return setting;
    }

    private EmployeeInsuranceSetting toInsuranceSetting(Long workplaceMemberId, UpdateEmployeeInsuranceSettingRequest request) {
        EmployeeInsuranceSetting setting = new EmployeeInsuranceSetting();
        setting.setWorkplaceMemberId(workplaceMemberId);
        setting.setUseNationalPension(request.getUseNationalPension());
        setting.setUseHealthInsurance(request.getUseHealthInsurance());
        setting.setUseLongTermCare(request.getUseLongTermCare());
        setting.setUseEmploymentInsurance(request.getUseEmploymentInsurance());
        setting.setUseIncomeTax(request.getUseIncomeTax());
        setting.setTaxFreeAmount(request.getTaxFreeAmount());
        return setting;
    }

    public CalculateDeductionsResponse calculateDeductions(Long ownerUserId, Long workplaceId, Long payslipId,
                                                           CalculateDeductionsRequest request) {
        ensureOwner(workplaceId, ownerUserId);
        Payslip payslip = findPayslipOrThrow(workplaceId, payslipId);

        WorkplaceMember member = workplaceMemberMapper.findMember(payslip.getWorkplaceId(), payslip.getUserId());
        if (member == null) {
            return new CalculateDeductionsResponse(List.of(), BigDecimal.ZERO);
        }
        EmployeeInsuranceSetting setting = employeeInsuranceSettingMapper.findByWorkplaceMemberId(member.getId());
        if (setting == null) {
            return new CalculateDeductionsResponse(List.of(), BigDecimal.ZERO);
        }

        BigDecimal whp = payslip.getWeeklyHolidayPay() != null ? payslip.getWeeklyHolidayPay() : BigDecimal.ZERO;
        BigDecimal totalPay = payslip.getBaseWage().add(whp).add(request.getBonusAmount());

        List<PayslipDeduction> deductions = insuranceCalculationHelper.calculate(setting, totalPay, payslip.getToDate());

        List<CalculateDeductionsResponse.DeductionItem> items = new ArrayList<>();
        BigDecimal totalDeduction = BigDecimal.ZERO;
        for (PayslipDeduction d : deductions) {
            items.add(new CalculateDeductionsResponse.DeductionItem(
                    d.getDeductionType(), d.getName(), d.getAmount(),
                    d.getAppliedRate(), d.getAppliedBaseAmount(), d.getDisplayOrder()
            ));
            totalDeduction = totalDeduction.add(d.getAmount());
        }
        return new CalculateDeductionsResponse(items, totalDeduction);
    }

    @Transactional
    public PayslipDetailResponse updatePayslipWithDeductions(Long ownerUserId, Long workplaceId, Long payslipId,
                                                             UpdatePayslipWithDeductionsRequest request) {
        ensureOwner(workplaceId, ownerUserId);
        Payslip payslip = findPayslipOrThrow(workplaceId, payslipId);
        if ("CANCELLED".equals(payslip.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, PAYSLIP_ALREADY_CANCELLED);
        }

        payslipDeductionMapper.deleteByPayslipId(payslipId);
        for (UpdatePayslipWithDeductionsRequest.DeductionItem item : request.getDeductions()) {
            PayslipDeduction deduction = new PayslipDeduction();
            deduction.setPayslipId(payslipId);
            deduction.setDeductionType(item.getDeductionType());
            deduction.setName(item.getName());
            deduction.setAmount(item.getAmount());
            deduction.setNote(item.getNote());
            deduction.setDisplayOrder(item.getDisplayOrder());
            payslipDeductionMapper.insert(deduction);
        }

        BigDecimal totalDeduction = request.getDeductions().stream()
                .map(UpdatePayslipWithDeductionsRequest.DeductionItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal whp = payslip.getWeeklyHolidayPay() != null ? payslip.getWeeklyHolidayPay() : BigDecimal.ZERO;
        BigDecimal totalWage = payslip.getBaseWage()
                .add(whp)
                .add(request.getBonusAmount())
                .subtract(totalDeduction);

        payslipMapper.updateBonus(payslipId, request.getBonusAmount(), request.getBonusNote(), totalWage);
        payslipMapper.updateDeductionSnapshot(payslipId, totalDeduction, totalWage);
        return toOwnerDetailResponse(payslipMapper.findById(payslipId));
    }

    @Transactional
    public CancelPayslipResponse cancelPayslip(Long ownerUserId, Long workplaceId, Long payslipId) {
        ensureOwner(workplaceId, ownerUserId);
        Payslip payslip = findPayslipOrThrow(workplaceId, payslipId);
        if ("CANCELLED".equals(payslip.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, PAYSLIP_ALREADY_CANCELLED);
        }
        payslipMapper.updateStatus(payslipId, "CANCELLED");
        return new CancelPayslipResponse(payslipId);
    }

    @Transactional
    public ConfirmPayslipResponse confirmPayslip(Long ownerUserId, Long workplaceId, Long payslipId) {
        ensureOwner(workplaceId, ownerUserId);
        Payslip payslip = findPayslipOrThrow(workplaceId, payslipId);
        if ("CANCELLED".equals(payslip.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, PAYSLIP_ALREADY_CANCELLED);
        }
        if ("CONFIRMED".equals(payslip.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, PAYSLIP_ALREADY_CONFIRMED);
        }
        payslipMapper.updateStatus(payslipId, "CONFIRMED");
        sendPayslipConfirmedPushSafely(payslip);
        return new ConfirmPayslipResponse(payslipId);
    }

    private void sendPayslipConfirmedPushSafely(Payslip payslip) {
        try {
            List<StaffReminderTarget> tokens = pushTokenMapper.findStaffPushTokensByUserIdAndWorkplaceId(
                    payslip.getUserId(), payslip.getWorkplaceId()
            );
            if (tokens.isEmpty()) return;

            List<FcmDto> fcmList = tokens.stream()
                    .map(t -> FcmDto.builder()
                            .pushSeq(0L)
                            .pushToken(t.getToken())
                            .title("\uD83C\uDF30 급여명세서 알림")
                            .content(t.getWorkplaceName() + " 급여명세서가 도착했습니다.")
                            .pushLink("")
                            .project(ProjectId.ALBAM.getMessage())
                            .build())
                    .collect(Collectors.toList());

            fcmService.sendMultiEachMessage(ProjectId.ALBAM.getMessage(), fcmList);
        } catch (Exception e) {
            log.warn("Payslip confirmed push send failed. payslipId={}", payslip.getId(), e);
        }
    }

    private List<PayslipRecordItem> buildRecords(Long workplaceId, WorkplaceMember member, WorkplaceSetting setting,
                                                  List<WorkplaceBreakPolicy> breakPolicies, BigDecimal hourlyWage,
                                                  LocalDate fromDate, LocalDate toDate) {
        List<AttendanceRecord> attendanceRecords = attendanceRecordMapper.findMyRecordsByPeriod(
                workplaceId, member.getUserId(), fromDate, toDate
        );
        Collections.reverse(attendanceRecords);

        List<PayslipRecordItem> result = new ArrayList<>();
        boolean memberIsDaily = "DAILY".equals(member.getWageType());
        for (AttendanceRecord record : attendanceRecords) {
            if (record.getCheckOutAt() == null) continue;
            int grossWorkedMinutes = calculateWorkedMinutes(record.getCheckInAt(), record.getCheckOutAt());
            if (memberIsDaily) {
                BigDecimal dw = member.getDailyWage() != null ? member.getDailyWage() : BigDecimal.ZERO;
                int payableMinutes = wageCalculationHelper.calculatePayableWorkedMinutes(grossWorkedMinutes, setting, breakPolicies, member.getBreakMinutes());
                result.add(new PayslipRecordItem(record.getWorkDate(), record.getCheckInAt().toLocalTime(), record.getCheckOutAt().toLocalTime(), payableMinutes, dw));
            } else {
                WageCalculationResult calc = wageCalculationHelper.calculate(hourlyWage, grossWorkedMinutes, setting, breakPolicies, member.getBreakMinutes());
                result.add(new PayslipRecordItem(record.getWorkDate(), record.getCheckInAt().toLocalTime(), record.getCheckOutAt().toLocalTime(), calc.workedMinutes(), calc.finalWage()));
            }
        }
        return result;
    }

    private Payslip findPayslipOrThrow(Long workplaceId, Long payslipId) {
        Payslip payslip = payslipMapper.findById(payslipId);
        if (payslip == null || !workplaceId.equals(payslip.getWorkplaceId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, PAYSLIP_NOT_FOUND);
        }
        return payslip;
    }

    private PayslipDetailResponse toOwnerDetailResponse(Payslip p) {
        List<PayslipDeductionItemResponse> deductions = payslipDeductionMapper.findByPayslipId(p.getId()).stream()
                .map(d -> new PayslipDeductionItemResponse(d.getId(), d.getDeductionType(), d.getName(), d.getAmount(), d.getNote(), d.getDisplayOrder()))
                .collect(Collectors.toList());
        BigDecimal weeklyHolidayPay = p.getWeeklyHolidayPay() != null ? p.getWeeklyHolidayPay() : BigDecimal.ZERO;
        return new PayslipDetailResponse(
                p.getId(), p.getUserId(), p.getUserName(), p.getProfileColor(),
                p.getFromDate(), p.getToDate(), p.getCreatedAt().toLocalDate(),
                p.getWorkedDays(), p.getWorkedMinutes(), p.getWageType(), p.getHourlyWage(), p.getMonthlyWage(),
                p.getDailyWage() != null ? p.getDailyWage() : BigDecimal.ZERO,
                p.getBaseWage(), weeklyHolidayPay, p.getBonusAmount(), p.getDeductionAmount(), p.getTotalWage(),
                p.getBonusNote(), deductions, deserializeSnapshot(p.getDailySnapshot())
        );
    }

    private String serializeSnapshot(List<PayslipRecordItem> records) {
        try {
            return objectMapper.writeValueAsString(records);
        } catch (Exception e) {
            throw new ApiException(INVALID_REQUEST);
        }
    }

    private List<PayslipRecordItem> deserializeSnapshot(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<PayslipRecordItem>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
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
        if (Boolean.TRUE.equals(request.getUseLocationRestriction())) {
            workplaceMapper.updateQrAttendance(workplaceId, false, false, null);
        }
    }

    @Transactional
    public void updateQrAttendance(Long ownerUserId, Long workplaceId, Boolean useQrAttendance, Boolean qrNoTimeLimit, String qrPin) {
        ensureOwner(workplaceId, ownerUserId);
        workplaceMapper.updateQrAttendance(workplaceId, useQrAttendance, Boolean.TRUE.equals(qrNoTimeLimit), qrPin);
        if (Boolean.TRUE.equals(useQrAttendance)) {
            workplaceMapper.updateLocationRestrictionFlag(workplaceId, false);
        }
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
    public void updateUseWeeklyHolidayPay(Long ownerUserId, Long workplaceId, Boolean useWeeklyHolidayPay) {
        ensureOwner(workplaceId, ownerUserId);
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        if (setting == null) {
            throw new ApiException(WORKPLACE_SETTING_NOT_FOUND);
        }
        workplaceSettingMapper.updateUseWeeklyHolidayPay(workplaceId, useWeeklyHolidayPay);
    }

    private BigDecimal calculateWeeklyHolidayPay(List<PayslipRecordItem> records, BigDecimal hourlyWage) {
        Map<LocalDate, Integer> weeklyMinutes = new LinkedHashMap<>();
        for (PayslipRecordItem record : records) {
            LocalDate weekStart = record.getWorkDate().with(DayOfWeek.MONDAY);
            weeklyMinutes.merge(weekStart, record.getWorkedMinutes(), Integer::sum);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (int workedMinutes : weeklyMinutes.values()) {
            if (workedMinutes < 900) continue; // 주 15시간(900분) 미만 제외
            int cappedMinutes = Math.min(workedMinutes, 2400); // 주 40시간(2400분) 상한
            // 주휴수당 = (주 근무분 / 2400) × 8시간 × 시급 = 주 근무분 × 시급 / 300
            BigDecimal pay = hourlyWage
                    .multiply(BigDecimal.valueOf(cappedMinutes))
                    .divide(BigDecimal.valueOf(300), 2, RoundingMode.HALF_UP);
            pay = pay.divide(BigDecimal.TEN, 0, RoundingMode.FLOOR).multiply(BigDecimal.TEN);
            total = total.add(pay);
        }
        return total;
    }

    @Transactional
    public int recalculateWages(Long ownerUserId, Long workplaceId, LocalDate fromDate, LocalDate toDate) {
        ensureOwner(workplaceId, ownerUserId);
        WorkplaceSetting setting = workplaceSettingMapper.findByWorkplaceId(workplaceId);
        List<WorkplaceBreakPolicy> breakPolicies = resolveBreakPolicies(workplaceId, setting);
        List<AttendanceRecord> records = attendanceRecordMapper.findCompletedRecordsByWorkplace(workplaceId, fromDate, toDate);
        for (AttendanceRecord record : records) {
            WorkplaceMember member = workplaceMemberMapper.findActiveMember(workplaceId, record.getUserId());
            String memberWt = member != null ? member.getWageType() : null;
            if ("MONTHLY".equals(memberWt) || "DAILY".equals(memberWt)) continue;
            BigDecimal hourlyWage = resolveHourlyWage(member, setting);
            int grossWorkedMinutes = calculateWorkedMinutes(record.getCheckInAt(), record.getCheckOutAt());
            WageCalculationResult wageCalculation = wageCalculationHelper.calculate(
                    hourlyWage,
                    grossWorkedMinutes,
                    setting,
                    breakPolicies,
                    member.getBreakMinutes()
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
        String approvedWageType = staffMember != null ? staffMember.getWageType() : null;
        int approvedWorkedMinutes;
        BigDecimal approvedBaseWage;
        BigDecimal approvedFinalWage;
        if ("DAILY".equals(approvedWageType) && newCheckOut != null) {
            BigDecimal dw = staffMember.getDailyWage() != null ? staffMember.getDailyWage() : BigDecimal.ZERO;
            approvedWorkedMinutes = wageCalculationHelper.calculatePayableWorkedMinutes(grossWorkedMinutes, setting, breakPolicies, staffMember.getBreakMinutes() != null ? staffMember.getBreakMinutes() : 0);
            approvedBaseWage = dw;
            approvedFinalWage = dw;
        } else {
            BigDecimal hourlyWage = "MONTHLY".equals(approvedWageType) ? BigDecimal.ZERO : resolveHourlyWage(staffMember, setting);
            WageCalculationResult wageCalculation = wageCalculationHelper.calculate(hourlyWage, grossWorkedMinutes, setting, breakPolicies, staffMember != null && staffMember.getBreakMinutes() != null ? staffMember.getBreakMinutes() : 0);
            approvedWorkedMinutes = wageCalculation.workedMinutes();
            approvedBaseWage = wageCalculation.baseWage();
            approvedFinalWage = wageCalculation.finalWage();
        }

        String attendanceStatus = newCheckOut == null ? "WORKING" : resolveCheckOutStatus(newCheckIn, setting);
        attendanceRecordMapper.updateByOwnerDecision(
                record.getId(),
                newCheckIn,
                newCheckOut,
                approvedWorkedMinutes,
                approvedBaseWage,
                approvedFinalWage,
                attendanceStatus,
                record.getNote()
        );
    }

    private String resolveCheckOutStatus(LocalDateTime checkInAt, WorkplaceSetting setting) {
        if (setting == null || setting.getDefaultCheckInTime() == null) {
            return "COMPLETED";
        }
        int graceMinutes = setting.getLateGraceMinutes() != null ? setting.getLateGraceMinutes() : 0;
        LocalDateTime checkInAtTruncated = checkInAt.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        LocalDateTime deadline = checkInAt.toLocalDate()
                .atTime(setting.getDefaultCheckInTime())
                .plusMinutes(graceMinutes);
        return checkInAtTruncated.isAfter(deadline) ? "LATE" : "COMPLETED";
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

    private WorkplaceMember ensureOwnerOrManager(Long workplaceId, Long userId) {
        User user = userMapper.findById(userId);
        if (user != null && "SUPER_ADMIN".equals(user.getUserType())) {
            return null;
        }
        WorkplaceMember member = workplaceMemberMapper.findActiveOwnerOrManagerMember(workplaceId, userId);
        if (member == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, OWNER_ACCESS_ONLY);
        }
        return member;
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
        if (member != null && member.getHourlyWage() != null) {
            return member.getHourlyWage();
        }
        if (setting != null && setting.getDefaultHourlyWage() != null) {
            return setting.getDefaultHourlyWage();
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
                int grossWorkedMinutes = calculateWorkedMinutes(request.getCheckInAt(), request.getCheckOutAt());
                int coWorkedMinutes;
                BigDecimal coBaseWage;
                BigDecimal coFinalWage;
                if ("DAILY".equals(member.getWageType())) {
                    BigDecimal dw = member.getDailyWage() != null ? member.getDailyWage() : BigDecimal.ZERO;
                    coWorkedMinutes = wageCalculationHelper.calculatePayableWorkedMinutes(grossWorkedMinutes, setting, breakPolicies, member.getBreakMinutes());
                    coBaseWage = dw;
                    coFinalWage = dw;
                } else {
                    BigDecimal hourlyWage = "MONTHLY".equals(member.getWageType()) ? BigDecimal.ZERO : resolveHourlyWage(member, setting);
                    WageCalculationResult wageCalculation = wageCalculationHelper.calculate(hourlyWage, grossWorkedMinutes, setting, breakPolicies, member.getBreakMinutes());
                    coWorkedMinutes = wageCalculation.workedMinutes();
                    coBaseWage = wageCalculation.baseWage();
                    coFinalWage = wageCalculation.finalWage();
                }
                attendanceRecordMapper.updateCheckOut(
                        record.getId(),
                        request.getCheckOutAt(),
                        coWorkedMinutes,
                        coBaseWage,
                        coFinalWage,
                        resolveCheckOutStatus(request.getCheckInAt(), setting)
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

    public QrTokenResponse generateQrToken(Long userId, Long workplaceId) {
        ensureOwner(workplaceId, userId);
        Workplace workplace = workplaceMapper.findById(workplaceId);
        if (Boolean.TRUE.equals(workplace.getQrNoTimeLimit())) {
            String token = jwtTokenProvider.createPermanentQrToken(workplaceId);
            return new QrTokenResponse(token, null);
        }
        String token = jwtTokenProvider.createQrToken(workplaceId);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtTokenProvider.getQrTokenExpirationSeconds());
        return new QrTokenResponse(token, expiresAt.toString());
    }
}
