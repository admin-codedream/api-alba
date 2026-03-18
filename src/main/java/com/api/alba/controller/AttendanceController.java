package com.api.alba.controller;

import com.api.alba.domain.AttendanceRecord;
import com.api.alba.dto.AttendanceCheckInRequest;
import com.api.alba.dto.AttendanceCheckOutRequest;
import com.api.alba.exception.ApiException;
import com.api.alba.security.UserPrincipal;
import com.api.alba.service.AttendanceService;
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
