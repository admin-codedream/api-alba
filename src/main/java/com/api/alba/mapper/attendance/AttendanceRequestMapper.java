package com.api.alba.mapper.attendance;

import com.api.alba.domain.attendance.AttendanceRequest;
import com.api.alba.dto.owner.AttendanceRequestListItemResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AttendanceRequestMapper {
    int insert(AttendanceRequest attendanceRequest);

    AttendanceRequest findById(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int countPendingByRecordAndUser(
            @Param("attendanceRecordId") Long attendanceRecordId,
            @Param("userId") Long userId
    );

    int countPendingByWorkplaceId(@Param("workplaceId") Long workplaceId);

    List<AttendanceRequestListItemResponse> findByWorkplaceId(
            @Param("workplaceId") Long workplaceId,
            @Param("status") String status
    );
}
