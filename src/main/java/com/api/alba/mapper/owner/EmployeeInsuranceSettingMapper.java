package com.api.alba.mapper.owner;

import com.api.alba.domain.owner.EmployeeInsuranceSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmployeeInsuranceSettingMapper {

    EmployeeInsuranceSetting findByWorkplaceMemberId(@Param("workplaceMemberId") Long workplaceMemberId);

    int insert(EmployeeInsuranceSetting setting);

    int update(EmployeeInsuranceSetting setting);
}