package com.api.alba.component;

import com.api.alba.dto.push.StaffReminderTarget;
import com.api.alba.firebase.FcmDto;
import com.api.alba.firebase.FcmService;
import com.api.alba.firebase.ProjectId;
import com.api.alba.mapper.push.PushTokenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceReminderScheduler {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final PushTokenMapper pushTokenMapper;
    private final FcmService fcmService;

    @Scheduled(cron = "0 * * * * *")
    public void sendAttendanceReminders() {
        // 현재 시각 기준 5분 후의 HH:mm → 해당 시각이 DEFAULT_CHECK_IN/OUT_TIME인 매장 대상
        String targetTime = LocalTime.now()
                .plusMinutes(5)
                .truncatedTo(ChronoUnit.MINUTES)
                .format(HH_MM);

        sendCheckInReminders(targetTime);
        sendCheckOutReminders(targetTime);
    }

    private void sendCheckInReminders(String targetTime) {
        List<StaffReminderTarget> targets = pushTokenMapper.findStaffForCheckInReminder(targetTime);
        if (targets.isEmpty()) return;

        List<FcmDto> fcmList = targets.stream()
                .map(t -> FcmDto.builder()
                        .pushSeq(0L)
                        .pushToken(t.getToken())
                        .title("출근 알림")
                        .content(t.getWorkplaceName() + " 출근 시간 5분 전입니다.")
                        .pushLink("")
                        .project(ProjectId.ALBAM.getMessage())
                        .build())
                .collect(Collectors.toList());

        log.info("[출근 알림] 발송 대상 {}명 (targetTime={})", fcmList.size(), targetTime);
        fcmService.sendMultiEachMessage(ProjectId.ALBAM.getMessage(), fcmList);
    }

    private void sendCheckOutReminders(String targetTime) {
        List<StaffReminderTarget> targets = pushTokenMapper.findStaffForCheckOutReminder(targetTime);
        if (targets.isEmpty()) return;

        List<FcmDto> fcmList = targets.stream()
                .map(t -> FcmDto.builder()
                        .pushSeq(0L)
                        .pushToken(t.getToken())
                        .title("퇴근 알림")
                        .content(t.getWorkplaceName() + " 퇴근 시간 5분 전입니다.")
                        .pushLink("")
                        .project(ProjectId.ALBAM.getMessage())
                        .build())
                .collect(Collectors.toList());

        log.info("[퇴근 알림] 발송 대상 {}명 (targetTime={})", fcmList.size(), targetTime);
        fcmService.sendMultiEachMessage(ProjectId.ALBAM.getMessage(), fcmList);
    }
}
