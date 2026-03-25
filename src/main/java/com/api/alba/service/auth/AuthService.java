package com.api.alba.service.auth;

import com.api.alba.domain.auth.PasswordResetCode;
import com.api.alba.domain.auth.User;
import com.api.alba.domain.auth.UserSocialAccount;
import com.api.alba.domain.owner.Workplace;
import com.api.alba.domain.settings.WorkplaceSetting;
import com.api.alba.domain.staff.WorkplaceMember;
import com.api.alba.dto.auth.AuthResponse;
import com.api.alba.dto.auth.LoginRequest;
import com.api.alba.dto.auth.PasswordResetConfirmRequest;
import com.api.alba.dto.auth.PasswordResetRequest;
import com.api.alba.dto.auth.SignUpRequest;
import com.api.alba.dto.auth.SocialLoginRequest;
import com.api.alba.dto.staff.MeResponse;
import com.api.alba.dto.staff.UserWorkplaceInfo;
import com.api.alba.email.EmailDto;
import com.api.alba.email.EmailForm;
import com.api.alba.email.EmailService;
import com.api.alba.exception.ApiException;
import com.api.alba.mapper.auth.PasswordResetCodeMapper;
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
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.api.alba.exception.ExceptionMessages.ACCOUNT_NOT_ACTIVE;
import static com.api.alba.exception.ExceptionMessages.INVALID_LOGIN_ID_OR_PASSWORD;
import static com.api.alba.exception.ExceptionMessages.INVALID_REQUEST;
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
    private static final int PASSWORD_RESET_EXPIRE_MINUTES = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String[] PROFILE_COLORS = {
            "#EF4444", "#F97316", "#F59E0B", "#EAB308", "#84CC16",
            "#22C55E", "#10B981", "#14B8A6", "#06B6D4", "#0EA5E9",
            "#3B82F6", "#6366F1", "#8B5CF6", "#A855F7", "#D946EF",
            "#EC4899", "#F43F5E", "#64748B", "#334155", "#0F172A"
    };

    private final UserMapper userMapper;
    private final UserSocialAccountMapper userSocialAccountMapper;
    private final PasswordResetCodeMapper passwordResetCodeMapper;
    private final WorkplaceMapper workplaceMapper;
    private final WorkplaceSettingMapper workplaceSettingMapper;
    private final WorkplaceMemberMapper workplaceMemberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

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

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userMapper.findByLoginId(request.getLoginId().trim());
        if (!isEligibleForPasswordReset(user)) {
            return;
        }

        String code = generateSixDigitCode();
        LocalDateTime now = LocalDateTime.now();

        passwordResetCodeMapper.deleteByUserId(user.getId());

        PasswordResetCode passwordResetCode = new PasswordResetCode();
        passwordResetCode.setUserId(user.getId());
        passwordResetCode.setEmail(user.getLoginId());
        passwordResetCode.setCode(code);
        passwordResetCode.setExpiresAt(now.plusMinutes(PASSWORD_RESET_EXPIRE_MINUTES));
        passwordResetCode.setUsedAt(null);
        passwordResetCodeMapper.insert(passwordResetCode);

        sendPasswordResetEmail(user, code);
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        User user = userMapper.findByLoginId(request.getLoginId().trim());
        if (!isEligibleForPasswordReset(user)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_REQUEST);
        }

        PasswordResetCode passwordResetCode = passwordResetCodeMapper.findValidCode(
                user.getId(),
                request.getCode().trim(),
                LocalDateTime.now()
        );
        if (passwordResetCode == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_REQUEST);
        }

        userMapper.updatePasswordHash(user.getId(), passwordEncoder.encode(request.getNewPassword()));
        passwordResetCodeMapper.markUsed(passwordResetCode.getId(), LocalDateTime.now());
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
    public void withdraw(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ApiException(USER_NOT_FOUND);
        }
        userMapper.updateStatus(userId, "INACTIVE");
    }

    private String resolveLoginId(SignUpRequest request) {
        if (request.hasSocialAccount()) {
            return request.getProviderUserId().trim();
        }
        return request.getLoginId().trim();
    }

    private boolean isEligibleForPasswordReset(User user) {
        return user != null
                && user.getPasswordHash() != null
                && "ACTIVE".equals(user.getStatus())
                && looksLikeEmail(user.getLoginId());
    }

    private boolean looksLikeEmail(String loginId) {
        return loginId != null && loginId.contains("@");
    }

    private void sendPasswordResetEmail(User user, String code) {
        String content = EmailForm.getAuthContent()
                .replace("#{이름}", user.getName() == null ? "" : user.getName())
                .replace("#{인증코드}", code);

        EmailDto emailDto = EmailDto.builder()
                .recipient(user.getLoginId())
                .emailTitle("알밤 비밀번호 재설정 인증코드")
                .emailContent(content)
                .build();
        emailService.sendEmail(emailDto);
    }

    private String generateSixDigitCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
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
        member.setMemo(null);
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
            return "개인 근무기록";
        }
        return name.trim() + "의 근무기록";
    }

    private String generateInviteCode() {
        String token = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return token.substring(0, 10);
    }
}
