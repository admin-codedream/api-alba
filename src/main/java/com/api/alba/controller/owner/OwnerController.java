package com.api.alba.controller.owner;

import com.api.alba.domain.attendance.AttendanceRecord;
import com.api.alba.domain.owner.Workplace;
import com.api.alba.dto.owner.AttendancePushSettingResponse;
import com.api.alba.dto.owner.AttendanceRequestListItemResponse;
import com.api.alba.dto.owner.BreakPoliciesResponse;
import com.api.alba.dto.owner.CreateWorkplaceRequest;
import com.api.alba.dto.owner.OwnerCreateAttendanceRecordRequest;
import com.api.alba.dto.owner.OwnerUpdateAttendanceRecordRequest;
import com.api.alba.dto.owner.DashboardTodayResponse;
import com.api.alba.dto.owner.OwnerDecisionRequest;
import com.api.alba.dto.owner.OwnerWorkplaceMemberResponse;
import com.api.alba.dto.owner.RecalculateWagesResponse;
import com.api.alba.dto.owner.SaveBreakPoliciesRequest;
import com.api.alba.dto.owner.SavePayslipDeductionsRequest;
import com.api.alba.dto.owner.UpdateAttendancePushEnabledRequest;
import com.api.alba.dto.owner.UpdateHourlyWageRequest;
import com.api.alba.dto.owner.UpdateLocationRestrictionRequest;
import com.api.alba.dto.owner.UpdateDefaultWorkTimeRequest;
import com.api.alba.dto.owner.MemberScheduleItemResponse;
import com.api.alba.dto.owner.SaveMemberScheduleRequest;
import com.api.alba.dto.owner.UpdateSalaryCalcUnitRequest;
import com.api.alba.dto.owner.UpdateWeeklyHolidayPayRequest;
import com.api.alba.dto.owner.UpdateWorkplaceNameRequest;
import com.api.alba.dto.owner.UpdateMemberBreakMinutesRequest;
import com.api.alba.dto.owner.UpdateMemberWeeklyHolidayPayRequest;
import com.api.alba.dto.owner.UpdateMemberRoleRequest;
import com.api.alba.dto.owner.UpdateMemberWageRequest;
import com.api.alba.dto.owner.UpdateWorkplaceMemberMemoRequest;
import com.api.alba.dto.owner.CancelPayslipResponse;
import com.api.alba.dto.owner.ConfirmPayslipResponse;
import com.api.alba.dto.owner.IssuePayslipRequest;
import com.api.alba.dto.owner.IssuePayslipResponse;
import com.api.alba.dto.owner.OwnerDailyAttendanceItemResponse;
import com.api.alba.dto.owner.OwnerMonthlyCalendarItemResponse;
import com.api.alba.dto.owner.PayslipDetailResponse;
import com.api.alba.dto.owner.PayslipListItemResponse;
import com.api.alba.dto.owner.PayslipResponse;
import com.api.alba.dto.owner.CalculateDeductionsRequest;
import com.api.alba.dto.owner.CalculateDeductionsResponse;
import com.api.alba.dto.owner.UpdatePayslipRequest;
import com.api.alba.dto.owner.UpdatePayslipWithDeductionsRequest;
import com.api.alba.dto.owner.UpdateQrAttendanceRequest;
import com.api.alba.dto.owner.EmployeeInsuranceSettingResponse;
import com.api.alba.dto.owner.UpdateEmployeeInsuranceSettingRequest;
import com.api.alba.dto.staff.EmployeeWageSummary;
import com.api.alba.dto.staff.InviteCodeResponse;
import com.api.alba.exception.ApiException;
import com.api.alba.security.UserPrincipal;
import com.api.alba.service.owner.OwnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static com.api.alba.exception.ExceptionMessages.AUTHENTICATION_REQUIRED;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
public class OwnerController {
    private final OwnerService ownerService;

    @PostMapping("/workplaces")
    @ResponseStatus(HttpStatus.CREATED)
    public Workplace createWorkplace(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateWorkplaceRequest request
    ) {
        return ownerService.createWorkplace(requiredPrincipal(principal), request);
    }

    @GetMapping("/workplaces/{workplaceId}/invite-code")
    public InviteCodeResponse inviteCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId
    ) {
        return ownerService.getInviteCode(requiredPrincipal(principal), workplaceId);
    }

    @GetMapping("/workplaces/{workplaceId}/dashboard/today")
    public DashboardTodayResponse dashboardToday(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId
    ) {
        return ownerService.getTodayDashboard(requiredPrincipal(principal), workplaceId);
    }

    @GetMapping("/workplaces/{workplaceId}/settings")
    public AttendancePushSettingResponse getAttendancePushSetting(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId
    ) {
        return ownerService.getAttendancePushSetting(requiredPrincipal(principal), workplaceId);
    }

    @GetMapping("/workplaces/{workplaceId}/members")
    public List<OwnerWorkplaceMemberResponse> members(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId
    ) {
        return ownerService.getWorkplaceMembers(requiredPrincipal(principal), workplaceId);
    }

    @DeleteMapping("/workplaces/{workplaceId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkplaceMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long memberId
    ) {
        ownerService.deleteWorkplaceMember(requiredPrincipal(principal), workplaceId, memberId);
    }

    @GetMapping("/workplaces/{workplaceId}/members/{memberId}/schedules")
    public List<MemberScheduleItemResponse> getMemberSchedules(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long memberId
    ) {
        return ownerService.getMemberSchedules(requiredPrincipal(principal), workplaceId, memberId);
    }

    @PutMapping("/workplaces/{workplaceId}/members/{memberId}/schedules")
    public List<MemberScheduleItemResponse> saveMemberSchedules(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody SaveMemberScheduleRequest request
    ) {
        return ownerService.saveMemberSchedules(requiredPrincipal(principal), workplaceId, memberId, request);
    }

    @GetMapping("/workplaces/{workplaceId}/members/{memberId}/payslip")
    public PayslipResponse getPayslip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long memberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ownerService.getPayslip(requiredPrincipal(principal), workplaceId, memberId, startDate, endDate);
    }

    @PostMapping("/workplaces/{workplaceId}/payslips")
    @ResponseStatus(HttpStatus.CREATED)
    public IssuePayslipResponse issuePayslips(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @Valid @RequestBody IssuePayslipRequest request
    ) {
        return ownerService.issuePayslips(requiredPrincipal(principal), workplaceId, request);
    }

    @GetMapping("/workplaces/{workplaceId}/payslips")
    public List<PayslipListItemResponse> getPayslips(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @RequestParam String yearMonth
    ) {
        return ownerService.getPayslips(requiredPrincipal(principal), workplaceId, yearMonth);
    }

    @GetMapping("/workplaces/{workplaceId}/payslips/{payslipId}")
    public PayslipDetailResponse getPayslipDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long payslipId
    ) {
        return ownerService.getPayslipDetail(requiredPrincipal(principal), workplaceId, payslipId);
    }

    @PutMapping("/workplaces/{workplaceId}/payslips/{payslipId}")
    public PayslipDetailResponse updatePayslip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long payslipId,
            @Valid @RequestBody UpdatePayslipRequest request
    ) {
        return ownerService.updatePayslip(requiredPrincipal(principal), workplaceId, payslipId, request);
    }

    @PutMapping("/workplaces/{workplaceId}/payslips/{payslipId}/deductions")
    public PayslipDetailResponse savePayslipDeductions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long payslipId,
            @Valid @RequestBody SavePayslipDeductionsRequest request
    ) {
        return ownerService.savePayslipDeductions(requiredPrincipal(principal), workplaceId, payslipId, request);
    }

    @PutMapping("/workplaces/{workplaceId}/payslips/{payslipId}/full")
    public PayslipDetailResponse updatePayslipWithDeductions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long payslipId,
            @Valid @RequestBody UpdatePayslipWithDeductionsRequest request
    ) {
        return ownerService.updatePayslipWithDeductions(requiredPrincipal(principal), workplaceId, payslipId, request);
    }

    @PostMapping("/workplaces/{workplaceId}/payslips/{payslipId}/calculate-deductions")
    public CalculateDeductionsResponse calculateDeductions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long payslipId,
            @Valid @RequestBody CalculateDeductionsRequest request
    ) {
        return ownerService.calculateDeductions(requiredPrincipal(principal), workplaceId, payslipId, request);
    }

    @DeleteMapping("/workplaces/{workplaceId}/payslips/{payslipId}")
    public CancelPayslipResponse cancelPayslip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long payslipId
    ) {
        return ownerService.cancelPayslip(requiredPrincipal(principal), workplaceId, payslipId);
    }

    @PatchMapping("/workplaces/{workplaceId}/payslips/{payslipId}/confirm")
    public ConfirmPayslipResponse confirmPayslip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long payslipId
    ) {
        return ownerService.confirmPayslip(requiredPrincipal(principal), workplaceId, payslipId);
    }

    @PatchMapping("/workplaces/{workplaceId}/members/{memberId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMemberRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request
    ) {
        ownerService.updateMemberRole(requiredPrincipal(principal), workplaceId, memberId, request.getRole());
    }

    @PatchMapping("/workplaces/{workplaceId}/members/{memberId}/wage")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMemberWage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberWageRequest request
    ) {
        ownerService.updateMemberWage(requiredPrincipal(principal), workplaceId, memberId, request);
    }

    @PatchMapping("/workplaces/{workplaceId}/members/{memberId}/break-minutes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMemberBreakMinutes(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberBreakMinutesRequest request
    ) {
        ownerService.updateMemberBreakMinutes(requiredPrincipal(principal), workplaceId, memberId, request.getBreakMinutes());
    }

    @PatchMapping("/workplaces/{workplaceId}/members/{memberId}/weekly-holiday-pay")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMemberUseWeeklyHolidayPay(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long memberId,
            @RequestBody UpdateMemberWeeklyHolidayPayRequest request
    ) {
        ownerService.updateMemberUseWeeklyHolidayPay(requiredPrincipal(principal), workplaceId, memberId, request.getUseWeeklyHolidayPay());
    }

    @PatchMapping("/workplaces/{workplaceId}/members/{memberId}/memo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMemberMemo(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateWorkplaceMemberMemoRequest request
    ) {
        ownerService.updateWorkplaceMemberMemo(requiredPrincipal(principal), workplaceId, memberId, request);
    }

    @GetMapping("/workplaces/{workplaceId}/attendance-records")
    public List<AttendanceRecord> attendanceRecords(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ownerService.getWorkplaceAttendanceRecords(requiredPrincipal(principal), workplaceId, userId, userName, fromDate, toDate);
    }

    @GetMapping("/workplaces/{workplaceId}/attendance-requests")
    public List<AttendanceRequestListItemResponse> attendanceRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @RequestParam(required = false) String status
    ) {
        return ownerService.getAttendanceRequests(requiredPrincipal(principal), workplaceId, status);
    }

    @PatchMapping("/attendance-requests/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decideRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long requestId,
            @Valid @RequestBody OwnerDecisionRequest request
    ) {
        ownerService.decideAttendanceRequest(requiredPrincipal(principal), requestId, request);
    }

    @GetMapping("/workplaces/{workplaceId}/calendar/daily")
    public List<OwnerDailyAttendanceItemResponse> dailyAttendance(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate
    ) {
        return ownerService.getDailyAttendance(requiredPrincipal(principal), workplaceId, workDate);
    }

    @GetMapping("/workplaces/{workplaceId}/calendar/monthly")
    public List<OwnerMonthlyCalendarItemResponse> staffMonthlyCalendar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @RequestParam String yearMonth,
            @RequestParam(required = false) Long userId
    ) {
        return ownerService.getStaffMonthlyCalendar(requiredPrincipal(principal), workplaceId, userId, yearMonth);
    }

    @GetMapping("/workplaces/{workplaceId}/wages/expected")
    public List<EmployeeWageSummary> expectedWageSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ownerService.getExpectedWageSummary(requiredPrincipal(principal), workplaceId, fromDate, toDate);
    }

    @PostMapping("/workplaces/{workplaceId}/attendance-records")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AttendanceRecord> createAttendanceRecord(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @Valid @RequestBody OwnerCreateAttendanceRecordRequest request
    ) {
        return ownerService.createAttendanceRecord(requiredPrincipal(principal), workplaceId, request);
    }

    @PatchMapping("/workplaces/{workplaceId}/attendance-records/{recordId}")
    public AttendanceRecord updateAttendanceRecord(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long recordId,
            @Valid @RequestBody OwnerUpdateAttendanceRecordRequest request
    ) {
        return ownerService.updateAttendanceRecord(requiredPrincipal(principal), workplaceId, recordId, request);
    }

    @PostMapping("/workplaces/{workplaceId}/attendance-records/{recordId}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttendanceRecord(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long recordId
    ) {
        ownerService.deleteAttendanceRecord(requiredPrincipal(principal), workplaceId, recordId);
    }

    @PostMapping("/workplaces/{workplaceId}/attendance-records/recalculate-wages")
    public RecalculateWagesResponse recalculateWages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        if (startDate != null && endDate != null) {
            int updatedCount = ownerService.recalculateWages(requiredPrincipal(principal), workplaceId, startDate, endDate);
            return new RecalculateWagesResponse(updatedCount);
        }
        YearMonth yearMonth = YearMonth.parse(month);
        int updatedCount = ownerService.recalculateWages(requiredPrincipal(principal), workplaceId, yearMonth.atDay(1), yearMonth.atEndOfMonth());
        return new RecalculateWagesResponse(updatedCount);
    }

    @GetMapping("/workplaces/{workplaceId}/break-policies")
    public BreakPoliciesResponse getBreakPolicies(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId
    ) {
        return ownerService.getBreakPolicies(requiredPrincipal(principal), workplaceId);
    }

    @PutMapping("/workplaces/{workplaceId}/break-policies")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveBreakPolicies(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @Valid @RequestBody SaveBreakPoliciesRequest request
    ) {
        ownerService.saveBreakPolicies(requiredPrincipal(principal), workplaceId, request);
    }

    @PatchMapping("/workplaces/{workplaceId}/settings/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateWorkplaceName(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @Valid @RequestBody UpdateWorkplaceNameRequest request
    ) {
        ownerService.updateWorkplaceName(requiredPrincipal(principal), workplaceId, request.getWorkplaceName());
    }

    @PatchMapping("/workplaces/{workplaceId}/settings/location-restriction")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLocationRestriction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @Valid @RequestBody UpdateLocationRestrictionRequest request
    ) {
        ownerService.updateLocationRestriction(requiredPrincipal(principal), workplaceId, request);
    }

    @PatchMapping("/workplaces/{workplaceId}/settings/qr-checkin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateQrAttendance(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @Valid @RequestBody UpdateQrAttendanceRequest request
    ) {
        ownerService.updateQrAttendance(requiredPrincipal(principal), workplaceId, request.getUseQrAttendance(), request.getQrNoTimeLimit(), request.getQrPin());
    }

    @PatchMapping("/workplaces/{workplaceId}/settings/attendance-push")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateAttendancePushEnabled(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @Valid @RequestBody UpdateAttendancePushEnabledRequest request
    ) {
        ownerService.updateAttendancePushEnabled(requiredPrincipal(principal), workplaceId, request.getEnabled());
    }

    @PatchMapping("/workplaces/{workplaceId}/settings/hourly-wage")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateHourlyWage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @Valid @RequestBody UpdateHourlyWageRequest request
    ) {
        ownerService.updateHourlyWage(requiredPrincipal(principal), workplaceId, request.getHourlyWage());
    }

    @PatchMapping("/workplaces/{workplaceId}/settings/salary-calc-unit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSalaryCalcUnit(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @Valid @RequestBody UpdateSalaryCalcUnitRequest request
    ) {
        ownerService.updateSalaryCalcUnit(requiredPrincipal(principal), workplaceId, request.getSalaryCalcUnit());
    }

    @PatchMapping("/workplaces/{workplaceId}/settings/default-work-time")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateDefaultWorkTime(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @RequestBody UpdateDefaultWorkTimeRequest request
    ) {
        ownerService.updateDefaultWorkTime(requiredPrincipal(principal), workplaceId, request.getDefaultCheckInTime(), request.getDefaultCheckOutTime());
    }

    @PatchMapping("/workplaces/{workplaceId}/settings/weekly-holiday-pay")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUseWeeklyHolidayPay(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @RequestBody UpdateWeeklyHolidayPayRequest request
    ) {
        ownerService.updateUseWeeklyHolidayPay(requiredPrincipal(principal), workplaceId, request.getUseWeeklyHolidayPay());
    }

    @GetMapping("/workplaces/{workplaceId}/members/{memberId}/insurance-settings")
    public EmployeeInsuranceSettingResponse getEmployeeInsuranceSetting(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long memberId
    ) {
        return ownerService.getEmployeeInsuranceSetting(requiredPrincipal(principal), workplaceId, memberId);
    }

    @PutMapping("/workplaces/{workplaceId}/members/{memberId}/insurance-settings")
    public EmployeeInsuranceSettingResponse updateEmployeeInsuranceSetting(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateEmployeeInsuranceSettingRequest request
    ) {
        return ownerService.updateEmployeeInsuranceSetting(requiredPrincipal(principal), workplaceId, memberId, request);
    }

    private Long requiredPrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED);
        }
        return principal.getUserId();
    }
}
