package com.api.alba.service.notice;

import com.api.alba.domain.notice.WorkplaceNotice;
import com.api.alba.dto.notice.WorkplaceNoticeCreateRequest;
import com.api.alba.dto.notice.WorkplaceNoticeUpdateRequest;
import com.api.alba.dto.push.StaffReminderTarget;
import com.api.alba.exception.ApiException;
import com.api.alba.firebase.FcmDto;
import com.api.alba.firebase.FcmService;
import com.api.alba.firebase.ProjectId;
import com.api.alba.mapper.notice.WorkplaceNoticeMapper;
import com.api.alba.mapper.push.PushTokenMapper;
import com.api.alba.mapper.staff.WorkplaceMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.api.alba.exception.ExceptionMessages.NOTICE_NOT_FOUND;
import static com.api.alba.exception.ExceptionMessages.OWNER_ACCESS_ONLY;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkplaceNoticeService {

    private final WorkplaceNoticeMapper workplaceNoticeMapper;
    private final WorkplaceMemberMapper workplaceMemberMapper;
    private final PushTokenMapper pushTokenMapper;
    private final FcmService fcmService;

    @Transactional
    public WorkplaceNotice create(Long userId, Long workplaceId, WorkplaceNoticeCreateRequest request) {
        validateOwner(workplaceId, userId);

        WorkplaceNotice notice = new WorkplaceNotice();
        notice.setWorkplaceId(workplaceId);
        notice.setAuthorId(userId);
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setPinned(request.isPinned());
        workplaceNoticeMapper.insert(notice);

        sendNoticePushSafely(workplaceId, notice);

        return workplaceNoticeMapper.findById(notice.getId());
    }

    public List<WorkplaceNotice> getList(Long userId, Long workplaceId) {
        validateMember(workplaceId, userId);
        return workplaceNoticeMapper.findAllByWorkplaceId(workplaceId);
    }

    public WorkplaceNotice getOne(Long userId, Long workplaceId, Long noticeId) {
        validateMember(workplaceId, userId);
        return getNoticeOrThrow(noticeId, workplaceId);
    }

    @Transactional
    public WorkplaceNotice update(Long userId, Long workplaceId, Long noticeId, WorkplaceNoticeUpdateRequest request) {
        validateOwner(workplaceId, userId);
        getNoticeOrThrow(noticeId, workplaceId);
        workplaceNoticeMapper.update(noticeId, request.getTitle(), request.getContent());
        return workplaceNoticeMapper.findById(noticeId);
    }

    @Transactional
    public WorkplaceNotice togglePin(Long userId, Long workplaceId, Long noticeId) {
        validateOwner(workplaceId, userId);
        WorkplaceNotice notice = getNoticeOrThrow(noticeId, workplaceId);
        workplaceNoticeMapper.updatePinned(noticeId, !Boolean.TRUE.equals(notice.getPinned()));
        return workplaceNoticeMapper.findById(noticeId);
    }

    @Transactional
    public void delete(Long userId, Long workplaceId, Long noticeId) {
        validateOwner(workplaceId, userId);
        getNoticeOrThrow(noticeId, workplaceId);
        workplaceNoticeMapper.deleteById(noticeId);
    }

    private WorkplaceNotice getNoticeOrThrow(Long noticeId, Long workplaceId) {
        WorkplaceNotice notice = workplaceNoticeMapper.findById(noticeId);
        if (notice == null || !workplaceId.equals(notice.getWorkplaceId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, NOTICE_NOT_FOUND);
        }
        return notice;
    }

    private void validateOwner(Long workplaceId, Long userId) {
        if (workplaceMemberMapper.findActiveOwnerMember(workplaceId, userId) == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, OWNER_ACCESS_ONLY);
        }
    }

    private void validateMember(Long workplaceId, Long userId) {
        if (workplaceMemberMapper.findActiveMember(workplaceId, userId) == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, OWNER_ACCESS_ONLY);
        }
    }

    private void sendNoticePushSafely(Long workplaceId, WorkplaceNotice notice) {
        try {
            List<StaffReminderTarget> targets = pushTokenMapper.findActiveStaffTokensByWorkplaceId(workplaceId);
            if (targets.isEmpty()) return;

            List<FcmDto> fcmList = targets.stream()
                    .map(t -> FcmDto.builder()
                            .pushSeq(0L)
                            .pushToken(t.getToken())
                            .title("[" + t.getWorkplaceName() + "] 공지사항")
                            .content(notice.getTitle())
                            .pushLink("")
                            .project(ProjectId.ALBAM.getMessage())
                            .build())
                    .collect(Collectors.toList());

            log.info("[매장 공지 푸시] workplaceId={}, 발송 대상 {}명", workplaceId, fcmList.size());
            fcmService.sendMultiEachMessage(ProjectId.ALBAM.getMessage(), fcmList);
        } catch (Exception e) {
            log.warn("[매장 공지 푸시] 발송 실패 workplaceId={}, message={}", workplaceId, e.getMessage(), e);
        }
    }
}