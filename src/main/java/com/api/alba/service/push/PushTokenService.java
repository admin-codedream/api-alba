package com.api.alba.service.push;

import com.api.alba.domain.push.PushToken;
import com.api.alba.dto.push.PushTokenDeactivateRequest;
import com.api.alba.dto.push.PushTokenUpsertRequest;
import com.api.alba.mapper.push.PushTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PushTokenService {
    private final PushTokenMapper pushTokenMapper;

    @Transactional
    public void upsert(Long userId, PushTokenUpsertRequest request) {
        String normalizedPlatform = request.getPlatform().toUpperCase();
        String token = request.getToken().trim();
        LocalDateTime now = LocalDateTime.now();

        PushToken existing = pushTokenMapper.findByToken(token);
        if (existing == null) {
            PushToken pushToken = new PushToken();
            pushToken.setUserId(userId);
            pushToken.setPlatform(normalizedPlatform);
            pushToken.setToken(token);
            pushToken.setIsActive(true);
            pushToken.setLastSeenAt(now);
            pushTokenMapper.insert(pushToken);
            return;
        }

        pushTokenMapper.updateRegistration(existing.getId(), userId, normalizedPlatform, now);
    }

    @Transactional
    public void deactivate(Long userId, PushTokenDeactivateRequest request) {
        String token = request.getToken().trim();
        pushTokenMapper.deactivateByUserIdAndToken(userId, token);
    }
}
