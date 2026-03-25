package com.api.alba.service.auth;

import com.api.alba.domain.auth.User;
import com.api.alba.domain.auth.UserSocialAccount;
import com.api.alba.domain.owner.Workplace;
import com.api.alba.domain.settings.WorkplaceSetting;
import com.api.alba.domain.staff.WorkplaceMember;
import com.api.alba.dto.auth.AuthResponse;
import com.api.alba.dto.auth.LoginRequest;
import com.api.alba.dto.auth.SignUpRequest;
import com.api.alba.dto.auth.SocialLoginRequest;
import com.api.alba.dto.staff.MeResponse;
import com.api.alba.dto.staff.UserWorkplaceInfo;
import com.api.alba.exception.ApiException;
import com.api.alba.mapper.auth.UserMapper;
import com.api.alba.mapper.auth.UserSocialAccountMapper;
import com.api.alba.mapper.owner.WorkplaceMapper;
import com.api.alba.mapper.settings.WorkplaceSettingMapper;
import com.api.alba.mapper.staff.WorkplaceMemberMapper;
import com.api.alba.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.api.alba.exception.ExceptionMessages.ACCOUNT_NOT_ACTIVE;
import static com.api.alba.exception.ExceptionMessages.INVALID_LOGIN_ID_OR_PASSWORD;
import static com.api.alba.exception.ExceptionMessages.LOGIN_ID_ALREADY_IN_USE;
import static com.api.alba.exception.ExceptionMessages.SOCIAL_ACCOUNT_ALREADY_CONNECTED_TO_ANOTHER_USER;
import static com.api.alba.exception.ExceptionMessages.SOCIAL_SIGNUP_REQUIRED;
import static com.api.alba.exception.ExceptionMessages.USER_NOT_FOUND;
import static com.api.alba.exception.ExceptionMessages.USER_NOT_FOUND_FOR_SOCIAL_ACCOUNT;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int DEFAULT_ALLOWED_RADIUS_METERS = 100;
    private static final boolean DEFAULT_USE_LOCATION_RESTRICTION = false;
    private static final BigDecimal DEFAULT_WORKPLACE_HOURLY_WAGE = BigDecimal.ZERO;
    private static final String[] PROFILE_COLORS = {
            "#EF4444", "#F97316", "#F59E0B", "#EAB308", "#84CC16",
            "#22C55E", "#10B981", "#14B8A6", "#06B6D4", "#0EA5E9",
            "#3B82F6", "#6366F1", "#8B5CF6", "#A855F7", "#D946EF",
            "#EC4899", "#F43F5E", "#64748B", "#334155", "#0F172A"
    };

    private final UserMapper userMapper;
    private final UserSocialAccountMapper userSocialAccountMapper;
    private final WorkplaceMapper workplaceMapper;
    private final WorkplaceSettingMapper workplaceSettingMapper;
    private final WorkplaceMemberMapper workplaceMemberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        String resolvedLoginId = resolveLoginId(request);

        if (userMapper.findByLoginId(resolvedLoginId) != null) {
            throw new ApiException(LOGIN_ID_ALREADY_IN_USE);
        }

        if (request.hasSocialAccount()) {
            String provider = request.getProvider().toUpperCase();
            UserSocialAccount existingSocialAccount =
                    userSocialAccountMapper.findByProviderAndProviderUserId(provider, request.getProviderUserId());
            if (existingSocialAccount != null) {
                throw new ApiException(SOCIAL_ACCOUNT_ALREADY_CONNECTED_TO_ANOTHER_USER);
            }
        }

        User user = new User();
        user.setLoginId(resolvedLoginId);
        user.setPasswordHash(request.getPassword() == null || request.getPassword().trim().isEmpty()
                ? null
                : passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setProfileInitial(resolveProfileInitial(request.getName()));
        user.setProfileColor(resolveProfileColor(resolvedLoginId, request.getName()));
        user.setUserType(request.getUserType().toUpperCase());
        user.setStatus("ACTIVE");
        userMapper.insert(user);

        if (request.hasSocialAccount()) {
            UserSocialAccount account = new UserSocialAccount();
            account.setUserId(user.getId());
            account.setProvider(request.getProvider().toUpperCase());
            account.setProviderUserId(request.getProviderUserId());
            account.setProviderEmail(request.getProviderEmail());
            account.setProviderName(request.getProviderName());
            userSocialAccountMapper.insert(account);
        }

        if ("PERSONAL".equals(user.getUserType())) {
            createPersonalWorkspace(user, request.getHourlyWage());
        }

        String token = jwtTokenProvider.createToken(user.getId(), user.getLoginId());
        return new AuthResponse(token, "Bearer", jwtTokenProvider.getExpirationSeconds());
    }

    private String resolveLoginId(SignUpRequest request) {
        if (request.hasSocialAccount()) {
            return request.getProviderUserId().trim();
        }
        return request.getLoginId().trim();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userMapper.findByLoginId(request.getLoginId());
        if (user == null || user.getPasswordHash() == null) {
            throw new ApiException(INVALID_LOGIN_ID_OR_PASSWORD);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ApiException(ACCOUNT_NOT_ACTIVE);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(INVALID_LOGIN_ID_OR_PASSWORD);
        }

        userMapper.updateLastLoginAt(user.getId(), LocalDateTime.now());
        String token = jwtTokenProvider.createToken(user.getId(), user.getLoginId());
        return new AuthResponse(token, "Bearer", jwtTokenProvider.getExpirationSeconds());
    }

    public MeResponse me(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ApiException(USER_NOT_FOUND);
        }
        List<UserWorkplaceInfo> workplaces = workplaceMemberMapper.findActiveWorkplacesByUserId(userId);
        String profileInitial = user.getProfileInitial() == null
                ? resolveProfileInitial(user.getName())
                : user.getProfileInitial();
        String profileColor = user.getProfileColor() == null
                ? resolveProfileColor(user.getLoginId(), user.getName())
                : user.getProfileColor();
        return new MeResponse(
                user.getId(),
                workplaces,
                user.getLoginId(),
                user.getName(),
                profileInitial,
                profileColor,
                user.getUserType(),
                user.getStatus()
        );
    }

    @Transactional
    public AuthResponse socialLogin(SocialLoginRequest request) {
        String provider = request.getProvider().toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        UserSocialAccount account =
                userSocialAccountMapper.findByProviderAndProviderUserId(provider, request.getProviderUserId());

        if (account == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, SOCIAL_SIGNUP_REQUIRED);
        }

        User user = userMapper.findById(account.getUserId());
        if (user == null) {
            throw new ApiException(USER_NOT_FOUND_FOR_SOCIAL_ACCOUNT);
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ApiException(ACCOUNT_NOT_ACTIVE);
        }

        userMapper.updateLastLoginAt(user.getId(), now);
        userSocialAccountMapper.updateLastLoginAt(account.getId(), now);

        String token = jwtTokenProvider.createToken(user.getId(), user.getLoginId());
        return new AuthResponse(token, "Bearer", jwtTokenProvider.getExpirationSeconds());
    }

    private String resolveProfileInitial(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        return name.trim().substring(0, 1);
    }

    private String resolveProfileColor(String loginId, String name) {
        String seed = (loginId == null ? "" : loginId) + "|" + (name == null ? "" : name);
        int idx = Math.floorMod(seed.hashCode(), PROFILE_COLORS.length);
        return PROFILE_COLORS[idx];
    }

    private void createPersonalWorkspace(User user, BigDecimal hourlyWage) {
        BigDecimal resolvedHourlyWage = hourlyWage == null ? DEFAULT_WORKPLACE_HOURLY_WAGE : hourlyWage;

        Workplace workplace = new Workplace();
        workplace.setOwnerId(user.getId());
        workplace.setName(resolvePersonalWorkplaceName(user.getName()));
        workplace.setAddress(null);
        workplace.setInviteCode(generateInviteCode());
        workplace.setLatitude(null);
        workplace.setLongitude(null);
        workplace.setAllowedRadiusMeters(DEFAULT_ALLOWED_RADIUS_METERS);
        workplace.setUseLocationRestriction(DEFAULT_USE_LOCATION_RESTRICTION);
        workplace.setIsPersonal(true);
        workplaceMapper.insert(workplace);

        WorkplaceMember member = new WorkplaceMember();
        member.setWorkplaceId(workplace.getId());
        member.setUserId(user.getId());
        member.setRole("OWNER");
        member.setHourlyWage(resolvedHourlyWage);
        member.setReceiveAttendancePush(false);
        member.setStatus("ACTIVE");
        workplaceMemberMapper.insert(member);

        WorkplaceSetting workplaceSetting = new WorkplaceSetting();
        workplaceSetting.setWorkplaceId(workplace.getId());
        workplaceSetting.setLateGraceMinutes(0);
        workplaceSetting.setSalaryCalcUnit("MINUTE");
        workplaceSetting.setRoundingPolicy("NONE");
        workplaceSetting.setDefaultHourlyWage(resolvedHourlyWage);
        workplaceSettingMapper.insert(workplaceSetting);
    }

    private String resolvePersonalWorkplaceName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "??洹쇰Т湲곕줉";
        }
        return name.trim() + "??洹쇰Т湲곕줉";
    }

    private String generateInviteCode() {
        String token = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return token.substring(0, 10);
    }
}
