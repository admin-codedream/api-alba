package com.api.alba.controller.staff;

import com.api.alba.dto.attendance.AttendanceCorrectionRequestCreateRequest;
import com.api.alba.dto.attendance.AttendanceNewRecordRequestCreateRequest;
import com.api.alba.dto.attendance.AttendanceRequestCreatedResponse;
import com.api.alba.dto.staff.JoinWorkplaceRequest;
import com.api.alba.dto.staff.JoinWorkplaceResponse;
import com.api.alba.dto.staff.StaffAttendanceRequestListItemResponse;
import com.api.alba.dto.staff.StaffHomeTodayResponse;
import com.api.alba.dto.staff.StaffMonthlyCalendarItemResponse;
import com.api.alba.dto.staff.StaffPayslipDetailResponse;
import com.api.alba.dto.staff.StaffPayslipListItemResponse;
import com.api.alba.dto.staff.StaffTodaySummaryResponse;
import com.api.alba.dto.staff.StaffWorkDetailResponse;
import com.api.alba.exception.ApiException;
import com.api.alba.security.UserPrincipal;
import com.api.alba.service.staff.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {
    private final StaffService staffService;

    @PostMapping("/workplaces/join")
    public JoinWorkplaceResponse joinWorkplace(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody JoinWorkplaceRequest request
    ) {
        return staffService.joinWorkplaceByInviteCode(requiredPrincipal(principal), request);
    }

    @GetMapping("/workplaces/{workplaceId}/home/today")
    public StaffHomeTodayResponse homeToday(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId
    ) {
        return staffService.getHomeToday(requiredPrincipal(principal), workplaceId);
    }

    @GetMapping("/workplaces/{workplaceId}/summary/today")
    public StaffTodaySummaryResponse todaySummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId
    ) {
        return staffService.getTodaySummary(requiredPrincipal(principal), workplaceId);
    }

    @GetMapping("/workplaces/{workplaceId}/calendar/monthly")
    public List<StaffMonthlyCalendarItemResponse> monthlyCalendar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @RequestParam String yearMonth
    ) {
        return staffService.getMonthlyCalendar(requiredPrincipal(principal), workplaceId, yearMonth);
    }

    @GetMapping("/workplaces/{workplaceId}/calendar/daily")
    public StaffWorkDetailResponse dailyDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate
    ) {
        return staffService.getWorkDetail(requiredPrincipal(principal), workplaceId, workDate);
    }

    @GetMapping("/workplaces/{workplaceId}/attendance-requests")
    public List<StaffAttendanceRequestListItemResponse> getMyAttendanceRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId
    ) {
        return staffService.getMyAttendanceRequests(requiredPrincipal(principal), workplaceId);
    }

    @PostMapping("/workplaces/{workplaceId}/attendance-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public AttendanceRequestCreatedResponse submitNewRecordRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @Valid @RequestBody AttendanceNewRecordRequestCreateRequest request
    ) {
        return staffService.submitNewRecordRequest(requiredPrincipal(principal), workplaceId, request);
    }

    @PostMapping("/attendance-records/{attendanceRecordId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public AttendanceRequestCreatedResponse submitCorrectionRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long attendanceRecordId,
            @Valid @RequestBody AttendanceCorrectionRequestCreateRequest request
    ) {
        return staffService.submitCorrectionRequest(requiredPrincipal(principal), attendanceRecordId, request);
    }

    @GetMapping("/payslips")
    public List<StaffPayslipListItemResponse> getMyPayslips(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long workplaceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return staffService.getMyPayslips(requiredPrincipal(principal), workplaceId, fromDate, toDate);
    }

    @GetMapping("/payslips/{payslipId}")
    public StaffPayslipDetailResponse getMyPayslipDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long payslipId
    ) {
        return staffService.getMyPayslipDetail(requiredPrincipal(principal), payslipId);
    }

    private Long requiredPrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED);
        }
        return principal.getUserId();
    }
}
