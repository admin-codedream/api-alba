package com.api.alba.controller.owner;

import com.api.alba.domain.attendance.AttendanceRecord;
import com.api.alba.domain.owner.Workplace;
import com.api.alba.dto.owner.AttendancePushSettingResponse;
import com.api.alba.dto.owner.AttendanceRequestListItemResponse;
import com.api.alba.dto.owner.BreakPoliciesResponse;
import com.api.alba.dto.owner.CreateWorkplaceRequest;
import com.api.alba.dto.owner.DashboardTodayResponse;
import com.api.alba.dto.owner.OwnerDecisionRequest;
import com.api.alba.dto.owner.OwnerWorkplaceMemberResponse;
import com.api.alba.dto.owner.RecalculateWagesResponse;
import com.api.alba.dto.owner.SaveBreakPoliciesRequest;
import com.api.alba.dto.owner.UpdateAttendancePushEnabledRequest;
import com.api.alba.dto.owner.UpdateHourlyWageRequest;
import com.api.alba.dto.owner.UpdateLocationRestrictionRequest;
import com.api.alba.dto.owner.UpdateSalaryCalcUnitRequest;
import com.api.alba.dto.owner.UpdateWorkplaceNameRequest;
import com.api.alba.dto.owner.UpdateWorkplaceMemberMemoRequest;
import com.api.alba.dto.owner.OwnerDailyAttendanceItemResponse;
import com.api.alba.dto.owner.OwnerMonthlyCalendarItemResponse;
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ownerService.getWorkplaceAttendanceRecords(requiredPrincipal(principal), workplaceId, userId, fromDate, toDate);
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

    @PostMapping("/workplaces/{workplaceId}/attendance-records/recalculate-wages")
    public RecalculateWagesResponse recalculateWages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @RequestParam String month
    ) {
        int updatedCount = ownerService.recalculateWages(requiredPrincipal(principal), workplaceId, YearMonth.parse(month));
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
        ownerService.updateLocationRestriction(requiredPrincipal(principal), workplaceId, request.getUseLocationRestriction());
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

    private Long requiredPrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED);
        }
        return principal.getUserId();
    }
}
