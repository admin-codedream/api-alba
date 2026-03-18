package com.api.alba.mapper;

import com.api.alba.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper {
    int insert(User user);

    User findByLoginId(@Param("loginId") String loginId);

    User findById(@Param("id") Long id);

    int updateLastLoginAt(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime lastLoginAt);
}
