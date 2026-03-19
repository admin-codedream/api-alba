package com.api.alba.mapper.attendance;

import com.api.alba.domain.attendance.AttendanceRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AttendanceRequestMapper {
    int insert(AttendanceRequest attendanceRequest);

    AttendanceRequest findById(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int countPendingByRecordAndUser(
            @Param("attendanceRecordId") Long attendanceRecordId,
            @Param("userId") Long userId
    );
}
