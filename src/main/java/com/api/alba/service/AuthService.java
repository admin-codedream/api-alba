package com.api.alba.service;

import com.api.alba.domain.User;
import com.api.alba.dto.AuthResponse;
import com.api.alba.dto.LoginRequest;
import com.api.alba.dto.MeResponse;
import com.api.alba.dto.SignUpRequest;
import com.api.alba.dto.SocialLoginRequest;
import com.api.alba.exception.ApiException;
import com.api.alba.mapper.UserMapper;
import com.api.alba.mapper.UserSocialAccountMapper;
import com.api.alba.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final UserSocialAccountMapper userSocialAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        if (userMapper.findByLoginId(request.getLoginId()) != null) {
            throw new ApiException("loginId is already in use.");
        }

        User user = new User();
        user.setLoginId(request.getLoginId());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setStatus("ACTIVE");
        userMapper.insert(user);

        String token = jwtTokenProvider.createToken(user.getId(), user.getLoginId());
        return new AuthResponse(token, "Bearer", jwtTokenProvider.getExpirationSeconds());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userMapper.findByLoginId(request.getLoginId());
        if (user == null || user.getPasswordHash() == null) {
            throw new ApiException("Invalid loginId or password.");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ApiException("Account is not active.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException("Invalid loginId or password.");
        }

        userMapper.updateLastLoginAt(user.getId(), LocalDateTime.now());
        String token = jwtTokenProvider.createToken(user.getId(), user.getLoginId());
        return new AuthResponse(token, "Bearer", jwtTokenProvider.getExpirationSeconds());
    }

    public MeResponse me(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ApiException("User not found.");
        }
        return new MeResponse(user.getId(), user.getLoginId(), user.getName(), user.getStatus());
    }

    @Transactional
    public AuthResponse socialLogin(SocialLoginRequest request) {
        String provider = request.getProvider().toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        com.api.alba.domain.UserSocialAccount account =
                userSocialAccountMapper.findByProviderAndProviderUserId(provider, request.getProviderUserId());

        User user;
        if (account == null) {
            user = new User();
            user.setName(resolveUserName(request));
            user.setStatus("ACTIVE");
            userMapper.insert(user);

            account = new com.api.alba.domain.UserSocialAccount();
            account.setUserId(user.getId());
            account.setProvider(provider);
            account.setProviderUserId(request.getProviderUserId());
            account.setProviderEmail(request.getProviderEmail());
            account.setProviderName(request.getProviderName());
            userSocialAccountMapper.insert(account);
        } else {
            user = userMapper.findById(account.getUserId());
            if (user == null) {
                throw new ApiException("User not found for social account.");
            }
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ApiException("Account is not active.");
        }

        userMapper.updateLastLoginAt(user.getId(), now);
        userSocialAccountMapper.updateLastLoginAt(account.getId(), now);

        String token = jwtTokenProvider.createToken(user.getId(), user.getLoginId());
        return new AuthResponse(token, "Bearer", jwtTokenProvider.getExpirationSeconds());
    }

    @Transactional
    public void connectSocial(Long userId, SocialLoginRequest request) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ApiException("User not found.");
        }

        String provider = request.getProvider().toUpperCase();

        com.api.alba.domain.UserSocialAccount existingByProviderId =
                userSocialAccountMapper.findByProviderAndProviderUserId(provider, request.getProviderUserId());
        if (existingByProviderId != null && !existingByProviderId.getUserId().equals(userId)) {
            throw new ApiException("This social account is already connected to another user.");
        }

        com.api.alba.domain.UserSocialAccount existingByUserProvider =
                userSocialAccountMapper.findByUserIdAndProvider(userId, provider);
        if (existingByUserProvider != null) {
            throw new ApiException("This provider is already connected.");
        }

        if (existingByProviderId == null) {
            com.api.alba.domain.UserSocialAccount account = new com.api.alba.domain.UserSocialAccount();
            account.setUserId(userId);
            account.setProvider(provider);
            account.setProviderUserId(request.getProviderUserId());
            account.setProviderEmail(request.getProviderEmail());
            account.setProviderName(request.getProviderName());
            userSocialAccountMapper.insert(account);
        }
    }

    private String resolveUserName(SocialLoginRequest request) {
        if (request.getProviderName() != null && !request.getProviderName().isBlank()) {
            return request.getProviderName();
        }
        String provider = request.getProvider().toUpperCase();
        String externalId = request.getProviderUserId();
        int suffixLength = Math.min(8, externalId.length());
        return provider + "_" + externalId.substring(externalId.length() - suffixLength);
    }
}
