package com.api.alba.controller.auth;

import com.api.alba.dto.auth.AuthResponse;
import com.api.alba.dto.auth.LoginRequest;
import com.api.alba.dto.auth.PasswordResetConfirmRequest;
import com.api.alba.dto.auth.PasswordResetRequest;
import com.api.alba.dto.auth.SignUpRequest;
import com.api.alba.dto.auth.SocialLoginRequest;
import com.api.alba.dto.staff.MeResponse;
import com.api.alba.exception.ApiException;
import com.api.alba.security.UserPrincipal;
import com.api.alba.service.auth.AuthService;
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

import static com.api.alba.exception.ExceptionMessages.AUTHENTICATION_REQUIRED;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/social/login")
    public AuthResponse socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        return authService.socialLogin(request);
    }

    @PostMapping("/password/reset/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request);
    }

    @PostMapping("/password/reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED);
        }
        return authService.me(principal.getUserId());
    }

    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdrawByPost(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED);
        }
        authService.withdraw(principal.getUserId());
    }
}
