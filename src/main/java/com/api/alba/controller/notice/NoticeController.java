package com.api.alba.controller.notice;

import com.api.alba.domain.notice.Notice;
import com.api.alba.dto.notice.NoticeCreateRequest;
import com.api.alba.dto.notice.NoticeUpdateRequest;
import com.api.alba.service.notice.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {
    private final NoticeService noticeService;

    @GetMapping
    public List<Notice> getNotices() {
        return noticeService.getNotices();
    }

    @GetMapping("/{noticeId}")
    public Notice getNotice(@PathVariable Long noticeId) {
        return noticeService.getNotice(noticeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Notice createNotice(@Valid @RequestBody NoticeCreateRequest request) {
        return noticeService.createNotice(request);
    }

    @PutMapping("/{noticeId}")
    public Notice updateNotice(
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeUpdateRequest request
    ) {
        return noticeService.updateNotice(noticeId, request);
    }

    @DeleteMapping("/{noticeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
    }
}
