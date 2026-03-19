package com.api.alba.mapper.push;

import com.api.alba.domain.push.PushToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface PushTokenMapper {
    int insert(PushToken pushToken);

    PushToken findByToken(@Param("token") String token);

    int updateRegistration(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("platform") String platform,
            @Param("lastSeenAt") LocalDateTime lastSeenAt
    );

    int deactivateByUserIdAndToken(@Param("userId") Long userId, @Param("token") String token);
}
