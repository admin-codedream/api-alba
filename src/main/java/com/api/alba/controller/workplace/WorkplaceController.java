package com.api.alba.controller.workplace;

import com.api.alba.dto.attendance.QrTokenResponse;
import com.api.alba.exception.ApiException;
import com.api.alba.security.UserPrincipal;
import com.api.alba.service.owner.OwnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.api.alba.exception.ExceptionMessages.AUTHENTICATION_REQUIRED;

@RestController
@RequestMapping("/api/workplaces")
@RequiredArgsConstructor
public class WorkplaceController {
    private final OwnerService ownerService;

    @GetMapping("/{workplaceId}/qr-token")
    public QrTokenResponse getQrToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId
    ) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED);
        }
        return ownerService.generateQrToken(principal.getUserId(), workplaceId);
    }
}
