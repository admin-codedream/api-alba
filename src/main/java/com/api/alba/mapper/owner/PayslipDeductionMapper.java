package com.api.alba.mapper.owner;

import com.api.alba.domain.owner.PayslipDeduction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PayslipDeductionMapper {
    int insert(PayslipDeduction deduction);

    List<PayslipDeduction> findByPayslipId(@Param("payslipId") Long payslipId);

    int deleteByPayslipId(@Param("payslipId") Long payslipId);
}