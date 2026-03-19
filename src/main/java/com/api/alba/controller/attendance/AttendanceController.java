package com.api.alba.controller.attendance;

import com.api.alba.domain.attendance.AttendanceRecord;
import com.api.alba.dto.attendance.AttendanceCheckInRequest;
import com.api.alba.dto.attendance.AttendanceCheckOutRequest;
import com.api.alba.exception.ApiException;
import com.api.alba.security.UserPrincipal;
import com.api.alba.service.attendance.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;

    // 출근을 등록합니다.
    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    public AttendanceRecord checkIn(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AttendanceCheckInRequest request
    ) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return attendanceService.checkIn(principal.getUserId(), request);
    }

    // 퇴근을 등록하고 근무 시간/급여를 계산합니다.
    @PostMapping("/check-out")
    public AttendanceRecord checkOut(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AttendanceCheckOutRequest request
    ) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return attendanceService.checkOut(principal.getUserId(), request);
    }

    // 본인의 기간별 근태 기록을 조회합니다.
    @GetMapping("/me")
    public List<AttendanceRecord> myRecords(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long workplaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return attendanceService.myRecords(principal.getUserId(), workplaceId, fromDate, toDate);
    }
}
