package com.api.alba.mapper.owner;

import com.api.alba.domain.owner.Workplace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkplaceMapper {
    int insert(Workplace workplace);

    Workplace findById(@Param("id") Long id);

    Workplace findByInviteCode(@Param("inviteCode") String inviteCode);
}
