package com.api.alba.mapper.staff;

import com.api.alba.domain.staff.WorkplaceMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkplaceMemberMapper {
    int insert(WorkplaceMember workplaceMember);

    WorkplaceMember findMember(
            @Param("workplaceId") Long workplaceId,
            @Param("userId") Long userId
    );

    WorkplaceMember findActiveMember(
            @Param("workplaceId") Long workplaceId,
            @Param("userId") Long userId
    );

    WorkplaceMember findActiveOwnerMember(
            @Param("workplaceId") Long workplaceId,
            @Param("userId") Long userId
    );

    WorkplaceMember findFirstActiveByUserId(@Param("userId") Long userId);

    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
