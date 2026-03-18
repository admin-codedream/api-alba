package com.api.alba.mapper;

import com.api.alba.domain.AttendanceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AttendanceRecordMapper {
    int insert(AttendanceRecord attendanceRecord);

    AttendanceRecord findByWorkplaceUserAndDate(
            @Param("workplaceId") Long workplaceId,
            @Param("userId") Long userId,
            @Param("workDate") LocalDate workDate
    );

    int updateCheckOut(
            @Param("id") Long id,
            @Param("checkOutAt") LocalDateTime checkOutAt,
            @Param("workedMinutes") Integer workedMinutes,
            @Param("baseWage") BigDecimal baseWage,
            @Param("finalWage") BigDecimal finalWage,
            @Param("status") String status
    );

    List<AttendanceRecord> findMyRecordsByPeriod(
            @Param("workplaceId") Long workplaceId,
            @Param("userId") Long userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
