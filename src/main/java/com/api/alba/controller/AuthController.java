package com.api.alba.controller;

import com.api.alba.dto.AuthResponse;
import com.api.alba.dto.LoginRequest;
import com.api.alba.dto.MeResponse;
import com.api.alba.dto.SignUpRequest;
import com.api.alba.dto.SocialLoginRequest;
import com.api.alba.exception.ApiException;
import com.api.alba.security.UserPrincipal;
import com.api.alba.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    // 회원가입 후 JWT를 발급합니다.
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request);
    }

    // 로그인 후 JWT를 발급합니다.
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // 소셜 계정으로 로그인하고 JWT를 발급합니다.
    @PostMapping("/social/login")
    public AuthResponse socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        return authService.socialLogin(request);
    }

    // 현재 로그인 계정에 소셜 계정을 연결합니다.
    @PostMapping("/social/connect")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void connectSocial(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SocialLoginRequest request
    ) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        authService.connectSocial(principal.getUserId(), request);
    }

    // 현재 로그인한 사용자 정보를 조회합니다.
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return authService.me(principal.getUserId());
    }
}
