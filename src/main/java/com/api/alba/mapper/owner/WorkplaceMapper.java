package com.api.alba.mapper.owner;

import com.api.alba.domain.owner.Workplace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkplaceMapper {
    int insert(Workplace workplace);

    Workplace findById(@Param("id") Long id);

    int updateName(@Param("id") Long id, @Param("name") String name);

    int updateLocationRestriction(@Param("id") Long id, @Param("useLocationRestriction") Boolean useLocationRestriction, @Param("address") String address, @Param("latitude") Double latitude, @Param("longitude") Double longitude);

    Workplace findByInviteCode(@Param("inviteCode") String inviteCode);
}
