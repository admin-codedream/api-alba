package com.api.alba.mapper.auth;

import com.api.alba.domain.auth.UserWithdrawalReason;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserWithdrawalReasonMapper {
    int insert(UserWithdrawalReason reason);

    List<UserWithdrawalReason> findAllByUserId(Long userId);
}