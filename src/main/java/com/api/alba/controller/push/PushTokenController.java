package com.api.alba.controller.push;

import com.api.alba.dto.push.PushTokenUpsertRequest;
import com.api.alba.exception.ApiException;
import com.api.alba.security.UserPrincipal;
import com.api.alba.service.push.PushTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static com.api.alba.exception.ExceptionMessages.AUTHENTICATION_REQUIRED;

@RestController
@RequestMapping("/api/push-tokens")
@RequiredArgsConstructor
public class PushTokenController {
    private final PushTokenService pushTokenService;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void upsert(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PushTokenUpsertRequest request
    ) {
        pushTokenService.upsert(requiredPrincipal(principal), request);
    }

    private Long requiredPrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED);
        }
        return principal.getUserId();
    }
}
