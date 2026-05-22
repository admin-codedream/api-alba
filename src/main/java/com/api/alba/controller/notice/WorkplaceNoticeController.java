package com.api.alba.controller.notice;

import com.api.alba.domain.notice.WorkplaceNotice;
import com.api.alba.dto.notice.WorkplaceNoticeCreateRequest;
import com.api.alba.dto.notice.WorkplaceNoticeUpdateRequest;
import com.api.alba.exception.ApiException;
import com.api.alba.security.UserPrincipal;
import com.api.alba.service.notice.WorkplaceNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

import static com.api.alba.exception.ExceptionMessages.AUTHENTICATION_REQUIRED;

@RestController
@RequestMapping("/api/workplaces/{workplaceId}/notices")
@RequiredArgsConstructor
public class WorkplaceNoticeController {

    private final WorkplaceNoticeService workplaceNoticeService;

    @GetMapping
    public List<WorkplaceNotice> getList(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId
    ) {
        return workplaceNoticeService.getList(requiredPrincipal(principal), workplaceId);
    }

    @GetMapping("/{noticeId}")
    public WorkplaceNotice getOne(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long noticeId
    ) {
        return workplaceNoticeService.getOne(requiredPrincipal(principal), workplaceId, noticeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkplaceNotice create(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @Valid @RequestBody WorkplaceNoticeCreateRequest request
    ) {
        return workplaceNoticeService.create(requiredPrincipal(principal), workplaceId, request);
    }

    @PutMapping("/{noticeId}")
    public WorkplaceNotice update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long noticeId,
            @Valid @RequestBody WorkplaceNoticeUpdateRequest request
    ) {
        return workplaceNoticeService.update(requiredPrincipal(principal), workplaceId, noticeId, request);
    }

    @PatchMapping("/{noticeId}/pin")
    public WorkplaceNotice togglePin(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long noticeId
    ) {
        return workplaceNoticeService.togglePin(requiredPrincipal(principal), workplaceId, noticeId);
    }

    @DeleteMapping("/{noticeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long workplaceId,
            @PathVariable Long noticeId
    ) {
        workplaceNoticeService.delete(requiredPrincipal(principal), workplaceId, noticeId);
    }

    private Long requiredPrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED);
        }
        return principal.getUserId();
    }
}