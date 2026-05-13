package com.api.alba.controller.staff;

import com.api.alba.domain.contract.LaborContract;
import com.api.alba.dto.contract.LaborContractRejectRequest;
import com.api.alba.exception.ApiException;
import com.api.alba.security.UserPrincipal;
import com.api.alba.service.contract.LaborContractService;
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
import java.util.List;

import static com.api.alba.exception.ExceptionMessages.AUTHENTICATION_REQUIRED;

@RestController
@RequestMapping("/api/staff/contracts")
@RequiredArgsConstructor
public class LaborContractStaffController {

    private final LaborContractService laborContractService;

    @GetMapping
    public List<LaborContract> getMyContracts(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return laborContractService.getMyContracts(requiredPrincipal(principal));
    }

    @GetMapping("/{contractId}")
    public LaborContract getMyContract(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long contractId
    ) {
        return laborContractService.getMyContract(requiredPrincipal(principal), contractId);
    }

    @PostMapping("/{contractId}/sign")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void signContract(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long contractId
    ) {
        laborContractService.signContract(requiredPrincipal(principal), contractId);
    }

    @PostMapping("/{contractId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectContract(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long contractId,
            @Valid @RequestBody LaborContractRejectRequest request
    ) {
        laborContractService.rejectContract(requiredPrincipal(principal), contractId, request);
    }

    private Long requiredPrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED);
        }
        return principal.getUserId();
    }
}