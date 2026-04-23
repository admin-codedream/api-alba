package com.api.alba.controller.notice;

import com.api.alba.domain.notice.Notice;
import com.api.alba.service.notice.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/latest")
    public Notice getLatestNotice() {
        return noticeService.getLatestNotice();
    }

    @GetMapping("/{noticeId}")
    public Notice getNotice(@PathVariable Long noticeId) {
        return noticeService.getNotice(noticeId);
    }
}
