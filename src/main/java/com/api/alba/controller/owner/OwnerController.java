package com.api.alba.controller.owner;

import com.api.alba.domain.attendance.AttendanceRecord;
import com.api.alba.domain.owner.Workplace;
import com.api.alba.dto.owner.AttendancePushSettingResponse;
import com.api.alba.dto.owner.AttendanceRequestListItemResponse;
import com.api.alba.dto.owner.CreateWorkplaceRequest;
import com.api.alba.dto.owner.DashboardTodayResponse;
import com.api.alba.dto.owner.OwnerDecisionRequest;
import com.api.alba.dto.owner.OwnerWorkplaceMemberResponse;
import com.api.alba.dto.owner.UpdateAttendancePushSettingRequest;
import com.api.alba.dto.owner.UpdateWorkplaceMemberMemoRequest;
import com.api.alba.dto.staff.EmployeeWageSummary;
import com.api.alba.dto.staff.InviteCodeResponse;
import com.api.alba.exception.ApiException;
import com.api.alba.security.UserPrincipal;
import com.api.alba.service.owner.OwnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;
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

    @GetMapping("/workplaces/{workplaceId}/wages/expected")
    public List<EmployeeWageSummary> expectedWageSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ownerService.getExpectedWageSummary(requiredPrincipal(principal), workplaceId, fromDate, toDate);
    }

    @PatchMapping("/workplaces/{workplaceId}/settings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateAttendancePushSetting(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @Valid @RequestBody UpdateAttendancePushSettingRequest request
    ) {
        ownerService.updateAttendancePushSetting(requiredPrincipal(principal), workplaceId, request);
    }

    private Long requiredPrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED);
        }
        return principal.getUserId();
    }
}
