package com.api.alba.mapper;

import com.api.alba.domain.AttendanceRecord;
import com.api.alba.dto.EmployeeWageSummary;
import com.api.alba.dto.MyAggregateSummary;
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

    AttendanceRecord findById(@Param("id") Long id);

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

    int countTodayCheckedIn(@Param("workplaceId") Long workplaceId, @Param("today") LocalDate today);

    int countTodayWorking(@Param("workplaceId") Long workplaceId, @Param("today") LocalDate today);

    List<AttendanceRecord> findWorkplaceRecordsByPeriod(
            @Param("workplaceId") Long workplaceId,
            @Param("userId") Long userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    List<EmployeeWageSummary> findEmployeeWageSummaryByPeriod(
            @Param("workplaceId") Long workplaceId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    MyAggregateSummary findMyAggregateSummary(
            @Param("workplaceId") Long workplaceId,
            @Param("userId") Long userId
    );

    int updateByOwnerDecision(
            @Param("id") Long id,
            @Param("checkInAt") LocalDateTime checkInAt,
            @Param("checkOutAt") LocalDateTime checkOutAt,
            @Param("workedMinutes") Integer workedMinutes,
            @Param("baseWage") BigDecimal baseWage,
            @Param("finalWage") BigDecimal finalWage,
            @Param("status") String status
    );
}
