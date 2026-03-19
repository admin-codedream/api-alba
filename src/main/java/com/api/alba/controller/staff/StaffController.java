package com.api.alba.controller.staff;

import com.api.alba.dto.attendance.AttendanceCorrectionRequestCreateRequest;
import com.api.alba.dto.attendance.AttendanceRequestCreatedResponse;
import com.api.alba.dto.staff.JoinWorkplaceRequest;
import com.api.alba.dto.staff.JoinWorkplaceResponse;
import com.api.alba.dto.staff.StaffHomeTodayResponse;
import com.api.alba.dto.staff.StaffTodaySummaryResponse;
import com.api.alba.exception.ApiException;
import com.api.alba.security.UserPrincipal;
import com.api.alba.service.staff.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {
    private final StaffService staffService;

    // 초대코드로 사업장에 합류합니다.
    @PostMapping("/workplaces/join")
    public JoinWorkplaceResponse joinWorkplace(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody JoinWorkplaceRequest request
    ) {
        return staffService.joinWorkplaceByInviteCode(requiredPrincipal(principal), request);
    }

    // 직원 홈(오늘 근무 상태/예상 급여) 정보를 조회합니다.
    @GetMapping("/workplaces/{workplaceId}/home/today")
    public StaffHomeTodayResponse homeToday(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId
    ) {
        return staffService.getHomeToday(requiredPrincipal(principal), workplaceId);
    }

    // 직원의 오늘/누적 요약 정보를 조회합니다.
    @GetMapping("/workplaces/{workplaceId}/summary/today")
    public StaffTodaySummaryResponse todaySummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId
    ) {
        return staffService.getTodaySummary(requiredPrincipal(principal), workplaceId);
    }

    // 근태 정정 요청을 생성합니다.
    @PostMapping("/attendance-records/{attendanceRecordId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public AttendanceRequestCreatedResponse submitCorrectionRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long attendanceRecordId,
            @Valid @RequestBody AttendanceCorrectionRequestCreateRequest request
    ) {
        return staffService.submitCorrectionRequest(requiredPrincipal(principal), attendanceRecordId, request);
    }

    private Long requiredPrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return principal.getUserId();
    }
}
