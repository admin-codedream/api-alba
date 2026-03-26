package com.api.alba.controller.terms;

import com.api.alba.domain.terms.Terms;
import com.api.alba.dto.terms.TermsAgreeRequest;
import com.api.alba.exception.ApiException;
import com.api.alba.security.UserPrincipal;
import com.api.alba.service.terms.TermsService;
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
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermsController {
    private final TermsService termsService;

    @GetMapping
    public List<Terms> getActiveTerms() {
        return termsService.getActiveTerms();
    }

    @GetMapping("/{termsType}")
    public Terms getActiveTermsByType(@PathVariable String termsType) {
        return termsService.getActiveTermsByType(termsType);
    }

    @PostMapping("/agree")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void agreeToTerms(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TermsAgreeRequest request
    ) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED);
        }
        termsService.agreeToTerms(principal.getUserId(), request);
    }
}