package com.api.alba.mapper;

import com.api.alba.domain.UserSocialAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface UserSocialAccountMapper {
    UserSocialAccount findByProviderAndProviderUserId(
            @Param("provider") String provider,
            @Param("providerUserId") String providerUserId
    );

    UserSocialAccount findByUserIdAndProvider(
            @Param("userId") Long userId,
            @Param("provider") String provider
    );

    int insert(UserSocialAccount account);

    int updateLastLoginAt(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime lastLoginAt);
}
