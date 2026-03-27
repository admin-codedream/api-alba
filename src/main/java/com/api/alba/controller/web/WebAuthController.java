package com.api.alba.controller.web;

import com.api.alba.dto.auth.LoginMethodResponse;
import com.api.alba.dto.auth.LoginRequest;
import com.api.alba.dto.auth.WebLoginMethodRequest;
import com.api.alba.dto.auth.WebLoginResponse;
import com.api.alba.dto.auth.WebOtpConfirmRequest;
import com.api.alba.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/web/auth")
@RequiredArgsConstructor
public class WebAuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public WebLoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.webLogin(request);
    }

    @PostMapping("/login-method")
    public LoginMethodResponse getLoginMethod(@Valid @RequestBody WebLoginMethodRequest request) {
        return authService.getWebLoginMethod(request);
    }

    @PostMapping("/otp/confirm")
    public WebLoginResponse confirmOtp(@Valid @RequestBody WebOtpConfirmRequest request) {
        return authService.confirmWebOtp(request);
    }

}
