package com.api.alba.service.notice;

import com.api.alba.domain.notice.Notice;
import com.api.alba.exception.ApiException;
import com.api.alba.mapper.notice.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.api.alba.exception.ExceptionMessages.NOTICE_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class NoticeService {
    private final NoticeMapper noticeMapper;

    public List<Notice> getNotices() {
        return noticeMapper.findAll();
    }

    public Notice getNotice(Long id) {
        Notice notice = noticeMapper.findById(id);
        if (notice == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, NOTICE_NOT_FOUND);
        }
        return notice;
    }
}
