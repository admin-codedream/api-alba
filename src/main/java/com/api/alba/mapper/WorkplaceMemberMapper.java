package com.api.alba.mapper;

import com.api.alba.domain.WorkplaceMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkplaceMemberMapper {
    WorkplaceMember findActiveMember(
            @Param("workplaceId") Long workplaceId,
            @Param("userId") Long userId
    );
}
