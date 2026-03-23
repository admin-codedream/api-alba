package com.api.alba.service.notice;

import com.api.alba.domain.notice.Notice;
import com.api.alba.dto.notice.NoticeCreateRequest;
import com.api.alba.dto.notice.NoticeUpdateRequest;
import com.api.alba.exception.ApiException;
import com.api.alba.mapper.notice.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Notice createNotice(NoticeCreateRequest request) {
        Notice notice = new Notice();
        notice.setTitle(request.getTitle().trim());
        notice.setContent(request.getContent().trim());
        notice.setPinned(Boolean.TRUE.equals(request.getPinned()));
        noticeMapper.insert(notice);
        return getNotice(notice.getId());
    }

    @Transactional
    public Notice updateNotice(Long id, NoticeUpdateRequest request) {
        Notice notice = getNotice(id);
        notice.setTitle(request.getTitle().trim());
        notice.setContent(request.getContent().trim());
        notice.setPinned(Boolean.TRUE.equals(request.getPinned()));
        noticeMapper.update(notice);
        return getNotice(id);
    }

    @Transactional
    public void deleteNotice(Long id) {
        int deleted = noticeMapper.delete(id);
        if (deleted == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, NOTICE_NOT_FOUND);
        }
    }
}
