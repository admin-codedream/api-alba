package com.api.alba.mapper.push;

import com.api.alba.domain.push.PushToken;
import com.api.alba.dto.push.OwnerPushTokenTarget;
import com.api.alba.dto.push.StaffReminderTarget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

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

    List<OwnerPushTokenTarget> findOwnerPushTokensByWorkplaceAndUserId(
            @Param("workplaceId") Long workplaceId,
            @Param("userId") Long userId
    );

    List<StaffReminderTarget> findStaffForCheckInReminder(@Param("targetTime") String targetTime);

    List<StaffReminderTarget> findStaffForCheckOutReminder(@Param("targetTime") String targetTime);

    List<StaffReminderTarget> findOwnersWithNoStaff();
}
