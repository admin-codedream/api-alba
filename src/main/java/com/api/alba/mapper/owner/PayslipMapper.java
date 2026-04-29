package com.api.alba.mapper.owner;

import com.api.alba.domain.owner.Payslip;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PayslipMapper {
    int insert(Payslip payslip);

    Payslip findById(@Param("id") Long id);

    List<Payslip> findByWorkplaceId(
            @Param("workplaceId") Long workplaceId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    List<Payslip> findByUserId(
            @Param("userId") Long userId,
            @Param("workplaceId") Long workplaceId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    int updateBonus(
            @Param("id") Long id,
            @Param("bonusAmount") BigDecimal bonusAmount,
            @Param("bonusNote") String bonusNote,
            @Param("totalWage") BigDecimal totalWage
    );

    int updateDeductionSnapshot(
            @Param("id") Long id,
            @Param("deductionAmount") BigDecimal deductionAmount,
            @Param("totalWage") BigDecimal totalWage
    );

    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
