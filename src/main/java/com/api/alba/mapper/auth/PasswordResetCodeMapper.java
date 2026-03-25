package com.api.alba.mapper.auth;

import com.api.alba.domain.auth.PasswordResetCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface PasswordResetCodeMapper {
    int insert(PasswordResetCode passwordResetCode);

    int deleteByUserId(@Param("userId") Long userId);

    PasswordResetCode findValidCode(
            @Param("userId") Long userId,
            @Param("code") String code,
            @Param("now") LocalDateTime now
    );

    int markUsed(@Param("id") Long id, @Param("usedAt") LocalDateTime usedAt);
}
