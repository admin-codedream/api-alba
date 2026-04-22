package com.api.alba.firebase;

import com.api.alba.exception.ApiException;
import com.api.alba.mapper.push.PushTokenMapper;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.api.alba.exception.ExceptionMessages.PUSH_SEND_FAILED;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {
    private final PushTokenMapper pushTokenMapper;

    /**
     * 푸쉬 전송 - 단일
     *
     * @param fcmDto
     * @return
     */
    @Async
    public void sendMessage(FcmDto fcmDto) {
        try {
            Message message = makeOnePushMessage(fcmDto);

            FirebaseApp projectApp = FirebaseApp.getInstance(fcmDto.getProject());
            FirebaseMessaging.getInstance(projectApp).send(message);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, PUSH_SEND_FAILED);
        }
    }

    /**
     * 푸쉬 전송 - 다중
     *
     * @param tokenList
     * @param fcmDto
     * @return
     */
    public void sendMultiMessage(
            List<String> tokenList,
            FcmDto fcmDto
    ) {
        try {
            if (!CollectionUtils.isEmpty(tokenList)) {
                MulticastMessage message = makeMultiPushMessage(tokenList, fcmDto);
                FirebaseApp projectApp = FirebaseApp.getInstance(fcmDto.getProject());
                FirebaseMessaging.getInstance(projectApp).sendEachForMulticast(message);
            }
        } catch (Exception e) {
            log.error("Fcm push error {}", e.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, PUSH_SEND_FAILED);
        }
    }

    /**
     * 푸쉬 전송 - 다건(고유내용)
     * @param project
     * @param fcmList
     */
    @Async
    public void sendMultiEachMessage(
            String project,
            List<FcmDto> fcmList
    ) {
        try {
            if (CollectionUtils.isEmpty(fcmList)) {
                return;
            }

            List<Message> messages = fcmList.stream()
                    .map(alarm -> Message.builder()
                            .setToken(alarm.getPushToken())
                            .setNotification(Notification.builder()
                                    .setTitle(alarm.getTitle())
                                    .setBody(alarm.getContent())
                                    .build())
                            .setApnsConfig(getApnsConfig(alarm))
                            .setAndroidConfig(getAndroidConfig())
                            .build()
                    )
                    .collect(Collectors.toList());

            FirebaseApp projectApp = FirebaseApp.getInstance(project);
            BatchResponse batchResponse = FirebaseMessaging.getInstance(projectApp).sendEach(messages);

            List<SendResponse> responses = batchResponse.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse response = responses.get(i);
                if (!response.isSuccessful()) {
                    MessagingErrorCode errorCode = response.getException().getMessagingErrorCode();
                    if (errorCode == MessagingErrorCode.UNREGISTERED
                            || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                        String invalidToken = fcmList.get(i).getPushToken();
                        try {
                            pushTokenMapper.deleteByToken(invalidToken);
                        } catch (Exception deleteEx) {
                            log.warn("Failed to delete invalid push token: {}", invalidToken, deleteEx);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Fcm sendMultiEachMessage error {}", e.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, PUSH_SEND_FAILED);
        }
    }

    /**
     * 푸쉬 메세지 생성 - 단일
     *
     * @param fcmDto
     * @return
     */
    private Message makeOnePushMessage(FcmDto fcmDto) {
        Map<String, String> paramMap = getParamMap(fcmDto);

        // IOS 설정
        ApnsConfig apnsConfig = getApnsConfig(fcmDto);

        // 안드로이드 설정
        AndroidConfig androidConfig = getAndroidConfig();

        return Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(fcmDto.getTitle())
                        .setBody(fcmDto.getContent())
                        .build())
                .setToken(fcmDto.getPushToken())
                .putAllData(paramMap)
                .setApnsConfig(apnsConfig)
                .setAndroidConfig(androidConfig)
                .build();
    }

    /**
     * 푸쉬 메세지 생성 - 다중
     *
     * @param tokenList
     * @param fcmDto
     * @return
     */
    private MulticastMessage makeMultiPushMessage(
            List<String> tokenList,
            FcmDto fcmDto
    ) {
        Map<String, String> paramMap = getParamMap(fcmDto);

        // IOS 설정
        ApnsConfig apnsConfig = getApnsConfig(fcmDto);

        // 안드로이드 설정
        AndroidConfig androidConfig = getAndroidConfig();

        return MulticastMessage.builder()
                .addAllTokens(tokenList)
                .setNotification(Notification.builder()
                        .setTitle(fcmDto.getTitle())
                        .setBody(fcmDto.getContent())
                        .build())
                .putAllData(paramMap)
                .setApnsConfig(apnsConfig)
                .setAndroidConfig(androidConfig)
                .build();
    }

    private Map<String, String> getParamMap(FcmDto fcmDto) {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("id", Long.toString(fcmDto.getPushSeq()));
        paramMap.put("title", fcmDto.getTitle());
        paramMap.put("content", fcmDto.getContent());
        paramMap.put("link", fcmDto.getPushLink());
        paramMap.put("badge", "1");
        return paramMap;
    }

    private ApnsConfig getApnsConfig(FcmDto fcmDto) {
        return ApnsConfig.builder()
                .putHeader("apns-priority", "5")
                .setAps(Aps.builder()
                        .setContentAvailable(true)
                        .setMutableContent(true)
                        .setSound("default")
                        .setBadge(1)
                        .setAlert(ApsAlert.builder()
                                .setTitle(fcmDto.getTitle())
                                .setBody(fcmDto.getContent()).build())
                        .build())
                .setFcmOptions(ApnsFcmOptions.builder().build())
                .build();
    }

    private AndroidConfig getAndroidConfig() {
        return AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .build();
    }


}
