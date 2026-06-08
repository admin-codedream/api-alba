package com.api.alba.service.auth;

import com.api.alba.domain.auth.PasswordResetCode;
import com.api.alba.domain.auth.User;
import com.api.alba.domain.auth.UserSocialAccount;
import com.api.alba.domain.auth.UserWithdrawalReason;
import com.api.alba.domain.owner.Workplace;
import com.api.alba.domain.settings.WorkplaceSetting;
import com.api.alba.domain.staff.WorkplaceMember;
import com.api.alba.domain.terms.Terms;
import com.api.alba.domain.terms.UserTermsAgreement;
import com.api.alba.dto.auth.AuthResponse;
import com.api.alba.dto.auth.LoginMethodResponse;
import com.api.alba.dto.auth.LoginRequest;
import com.api.alba.dto.auth.PasswordResetConfirmRequest;
import com.api.alba.dto.auth.PasswordResetRequest;
import com.api.alba.dto.auth.SignUpRequest;
import com.api.alba.dto.auth.SocialLoginRequest;
import com.api.alba.dto.auth.WebLoginMethodRequest;
import com.api.alba.dto.auth.WebLoginResponse;
import com.api.alba.dto.auth.WebOtpConfirmRequest;
import com.api.alba.dto.auth.WithdrawRequest;
import com.api.alba.dto.staff.MeResponse;
import com.api.alba.dto.staff.UserWorkplaceInfo;
import com.api.alba.email.EmailDto;
import com.api.alba.email.EmailForm;
import com.api.alba.email.EmailService;
import com.api.alba.exception.ApiException;
import com.api.alba.mapper.auth.PasswordResetCodeMapper;
import com.api.alba.mapper.auth.UserMapper;
import com.api.alba.mapper.auth.UserSocialAccountMapper;
import com.api.alba.mapper.auth.UserWithdrawalReasonMapper;
import com.api.alba.mapper.owner.WorkplaceMapper;
import com.api.alba.mapper.settings.WorkplaceSettingMapper;
import com.api.alba.mapper.staff.WorkplaceMemberMapper;
import com.api.alba.mapper.terms.TermsMapper;
import com.api.alba.mapper.terms.UserTermsAgreementMapper;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.api.alba.exception.ExceptionMessages.ACCOUNT_NOT_ACTIVE;
import static com.api.alba.exception.ExceptionMessages.INVALID_LOGIN_ID_OR_PASSWORD;
import static com.api.alba.exception.ExceptionMessages.INVALID_OTP;
import static com.api.alba.exception.ExceptionMessages.INVALID_REQUEST;
import static com.api.alba.exception.ExceptionMessages.INVALID_WITHDRAWAL_REASON_TYPE;
import static com.api.alba.exception.ExceptionMessages.NO_REGISTERED_WORKPLACE;
import static com.api.alba.exception.ExceptionMessages.WEB_ACCESS_OWNER_ONLY;
import static com.api.alba.exception.ExceptionMessages.LOGIN_ID_ALREADY_IN_USE;
import static com.api.alba.exception.ExceptionMessages.REQUIRED_TERMS_NOT_ALL_AGREED;
import static com.api.alba.exception.ExceptionMessages.SOCIAL_ACCOUNT_ALREADY_CONNECTED_TO_ANOTHER_USER;
import static com.api.alba.exception.ExceptionMessages.SOCIAL_SIGNUP_REQUIRED;
import static com.api.alba.exception.ExceptionMessages.TERMS_NOT_FOUND;
import static com.api.alba.exception.ExceptionMessages.USER_NOT_FOUND;
import static com.api.alba.exception.ExceptionMessages.USER_NOT_FOUND_FOR_SOCIAL_ACCOUNT;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int DEFAULT_ALLOWED_RADIUS_METERS = 100;
    private static final boolean DEFAULT_USE_LOCATION_RESTRICTION = false;
    private static final BigDecimal DEFAULT_WORKPLACE_HOURLY_WAGE = BigDecimal.ZERO;
    private static final String DEFAULT_SALARY_CALC_UNIT = "10MIN";
    private static final String DEFAULT_ROUNDING_POLICY = "NONE";
    private static final int PASSWORD_RESET_EXPIRE_MINUTES = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String[] PROFILE_COLORS = {
            // Red
            "#EF4444", "#DC2626", "#F87171", "#B91C1C",
            // Orange
            "#F97316", "#EA580C", "#FB923C",
            // Amber
            "#F59E0B", "#D97706", "#FBBF24",
            // Yellow
            "#EAB308", "#CA8A04",
            // Lime
            "#84CC16", "#65A30D",
            // Green
            "#22C55E", "#16A34A", "#4ADE80", "#15803D",
            // Emerald
            "#10B981", "#059669", "#34D399",
            // Teal
            "#14B8A6", "#0D9488", "#2DD4BF",
            // Cyan
            "#06B6D4", "#0891B2", "#22D3EE",
            // Sky
            "#0EA5E9", "#0284C7", "#38BDF8",
            // Blue
            "#3B82F6", "#2563EB", "#60A5FA", "#1D4ED8",
            // Indigo
            "#6366F1", "#4F46E5", "#818CF8",
            // Violet
            "#8B5CF6", "#7C3AED", "#A78BFA",
            // Purple
            "#A855F7", "#9333EA", "#C084FC",
            // Fuchsia
            "#D946EF", "#C026D3", "#E879F9",
            // Pink
            "#EC4899", "#DB2777",
            // Rose
            "#F43F5E", "#E11D48",
            // Slate
            "#64748B"
    };

    private final UserMapper userMapper;
    private final UserSocialAccountMapper userSocialAccountMapper;
    private final PasswordResetCodeMapper passwordResetCodeMapper;
    private final UserWithdrawalReasonMapper userWithdrawalReasonMapper;
    private final WorkplaceMapper workplaceMapper;
    private final WorkplaceSettingMapper workplaceSettingMapper;
    private final WorkplaceMemberMapper workplaceMemberMapper;
    private final TermsMapper termsMapper;
    private final UserTermsAgreementMapper userTermsAgreementMapper;
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

        agreeToTerms(user.getId(), request.getTermsIds());

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
    public WebLoginResponse webLogin(LoginRequest request) {
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
        if (!isWebAccessibleUserType(user.getUserType())) {
            throw new ApiException(HttpStatus.FORBIDDEN, WEB_ACCESS_OWNER_ONLY);
        }

        List<WebLoginResponse.WorkplaceItem> workplaces = workplaceMemberMapper
                .findActiveWorkplacesByUserId(user.getId())
                .stream()
                .map(w -> new WebLoginResponse.WorkplaceItem(w.getWorkplaceId(), w.getWorkplaceName(), w.getIsPersonal()))
                .collect(Collectors.toList());

        if ("OWNER".equalsIgnoreCase(user.getUserType()) && workplaces.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, NO_REGISTERED_WORKPLACE);
        }

        userMapper.updateLastLoginAt(user.getId(), LocalDateTime.now());
        String token = jwtTokenProvider.createToken(user.getId(), user.getLoginId());

        return new WebLoginResponse(token, "Bearer", jwtTokenProvider.getExpirationSeconds(), user.getUserType(), workplaces);
    }

    @Transactional
    public LoginMethodResponse getWebLoginMethod(WebLoginMethodRequest request) {
        String email = request.getEmail().trim();

        User userByLoginId = userMapper.findByLoginId(email);
        if (userByLoginId != null && userByLoginId.getPasswordHash() != null) {
            return new LoginMethodResponse("PASSWORD");
        }

        UserSocialAccount socialAccount = userSocialAccountMapper.findByProviderEmail(email);
        if (socialAccount != null) {
            User socialUser = userMapper.findById(socialAccount.getUserId());
            if (socialUser != null && "ACTIVE".equals(socialUser.getStatus())) {
                sendWebOtp(socialUser, email);
            }
            return new LoginMethodResponse("OTP");
        }

        return new LoginMethodResponse("PASSWORD");
    }

    @Transactional
    public WebLoginResponse confirmWebOtp(WebOtpConfirmRequest request) {
        String email = request.getEmail().trim();

        UserSocialAccount socialAccount = userSocialAccountMapper.findByProviderEmail(email);
        if (socialAccount == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_OTP);
        }

        User user = userMapper.findById(socialAccount.getUserId());
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_OTP);
        }

        PasswordResetCode otpCode = passwordResetCodeMapper.findValidCode(
                user.getId(),
                request.getCode().trim(),
                LocalDateTime.now()
        );
        if (otpCode == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_OTP);
        }

        if (!isWebAccessibleUserType(user.getUserType())) {
            throw new ApiException(HttpStatus.FORBIDDEN, WEB_ACCESS_OWNER_ONLY);
        }

        List<WebLoginResponse.WorkplaceItem> workplaces = workplaceMemberMapper
                .findActiveWorkplacesByUserId(user.getId())
                .stream()
                .map(w -> new WebLoginResponse.WorkplaceItem(w.getWorkplaceId(), w.getWorkplaceName(), w.getIsPersonal()))
                .collect(Collectors.toList());

        if ("OWNER".equalsIgnoreCase(user.getUserType()) && workplaces.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, NO_REGISTERED_WORKPLACE);
        }

        passwordResetCodeMapper.markUsed(otpCode.getId(), LocalDateTime.now());
        userMapper.updateLastLoginAt(user.getId(), LocalDateTime.now());

        String token = jwtTokenProvider.createToken(user.getId(), user.getLoginId());

        return new WebLoginResponse(token, "Bearer", jwtTokenProvider.getExpirationSeconds(), user.getUserType(), workplaces);
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
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ApiException(USER_NOT_FOUND);
        }
        if (!request.isValidReasonType()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_WITHDRAWAL_REASON_TYPE);
        }

        UserWithdrawalReason reason = new UserWithdrawalReason();
        reason.setUserId(userId);
        reason.setReasonType(request.getReasonType().toUpperCase());
        reason.setCustomReason(
                request.getCustomReason() == null || request.getCustomReason().isBlank()
                        ? null
                        : request.getCustomReason().trim()
        );
        userWithdrawalReasonMapper.insert(reason);

        String anonymizedLoginId = "DELETED_" + userId + "_" + System.currentTimeMillis();
        userMapper.anonymizeUser(userId, anonymizedLoginId);
        userMapper.updateStatus(userId, "INACTIVE");
        userSocialAccountMapper.deleteByUserId(userId);
    }

    private void agreeToTerms(Long userId, List<Long> termsIds) {
        List<Terms> activeTerms = termsMapper.findActiveAll();

        Set<Long> requiredTermsIds = activeTerms.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsRequired()))
                .map(Terms::getId)
                .collect(Collectors.toSet());

        Set<Long> requestedIds = termsIds.stream().collect(Collectors.toSet());
        if (!requestedIds.containsAll(requiredTermsIds)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, REQUIRED_TERMS_NOT_ALL_AGREED);
        }

        for (Long termsId : termsIds) {
            Terms terms = termsMapper.findById(termsId);
            if (terms == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, TERMS_NOT_FOUND);
            }
            UserTermsAgreement agreement = new UserTermsAgreement();
            agreement.setUserId(userId);
            agreement.setTermsId(termsId);
            userTermsAgreementMapper.insert(agreement);
        }
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

    private boolean isWebAccessibleUserType(String userType) {
        return "OWNER".equalsIgnoreCase(userType) || "SUPER_ADMIN".equalsIgnoreCase(userType);
    }

    private boolean looksLikeEmail(String loginId) {
        return loginId != null && loginId.contains("@");
    }

    private void sendWebOtp(User user, String email) {
        String code = generateSixDigitCode();
        LocalDateTime now = LocalDateTime.now();

        passwordResetCodeMapper.deleteByUserId(user.getId());

        PasswordResetCode otpCode = new PasswordResetCode();
        otpCode.setUserId(user.getId());
        otpCode.setEmail(email);
        otpCode.setCode(code);
        otpCode.setExpiresAt(now.plusMinutes(PASSWORD_RESET_EXPIRE_MINUTES));
        otpCode.setUsedAt(null);
        passwordResetCodeMapper.insert(otpCode);

        String content = EmailForm.getAuthContent()
                .replace("#{이름}", user.getName() == null ? "" : user.getName())
                .replace("#{인증코드}", code);

        EmailDto emailDto = EmailDto.builder()
                .recipient(email)
                .emailTitle("알밤 웹 로그인 인증번호")
                .emailContent(content)
                .build();
        emailService.sendEmail(emailDto);
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
        workplace.setUseQrAttendance(false);
        workplace.setIsPersonal(true);
        workplaceMapper.insert(workplace);

        WorkplaceMember member = new WorkplaceMember();
        member.setWorkplaceId(workplace.getId());
        member.setUserId(user.getId());
        member.setRole("OWNER");
        member.setWageType("HOURLY");
        member.setHourlyWage(resolvedHourlyWage);
        member.setMemo(null);
        member.setReceiveAttendancePush(false);
        member.setStatus("ACTIVE");
        workplaceMemberMapper.insert(member);

        WorkplaceSetting workplaceSetting = new WorkplaceSetting();
        workplaceSetting.setWorkplaceId(workplace.getId());
        workplaceSetting.setLateGraceMinutes(0);
        workplaceSetting.setSalaryCalcUnit(DEFAULT_SALARY_CALC_UNIT);
        workplaceSetting.setRoundingPolicy(DEFAULT_ROUNDING_POLICY);
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
