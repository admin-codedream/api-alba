package com.api.alba.controller.owner;

import com.api.alba.domain.contract.LaborContract;
import com.api.alba.dto.contract.LaborContractCreateRequest;
import com.api.alba.exception.ApiException;
import com.api.alba.security.UserPrincipal;
import com.api.alba.service.contract.LaborContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/owner/workplaces/{workplaceId}/contracts")
@RequiredArgsConstructor
public class LaborContractOwnerController {

    private final LaborContractService laborContractService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LaborContract createContract(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @Valid @RequestBody LaborContractCreateRequest request
    ) {
        return laborContractService.createContract(requiredPrincipal(principal), workplaceId, request);
    }

    @PostMapping("/{contractId}/send")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendContract(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long contractId
    ) {
        laborContractService.sendContract(requiredPrincipal(principal), workplaceId, contractId);
    }

    @DeleteMapping("/{contractId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelContract(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long contractId
    ) {
        laborContractService.cancelContract(requiredPrincipal(principal), workplaceId, contractId);
    }

    @GetMapping
    public List<LaborContract> getContracts(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId
    ) {
        return laborContractService.getContracts(requiredPrincipal(principal), workplaceId);
    }

    @GetMapping("/{contractId}")
    public LaborContract getContract(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long contractId
    ) {
        return laborContractService.getContract(requiredPrincipal(principal), workplaceId, contractId);
    }

    private Long requiredPrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED);
        }
        return principal.getUserId();
    }
}