package com.api.alba.mapper.contract;

import com.api.alba.domain.contract.LaborContract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LaborContractMapper {
    int insert(LaborContract contract);

    LaborContract findById(@Param("id") Long id);

    List<LaborContract> findByWorkplaceId(@Param("workplaceId") Long workplaceId);

    List<LaborContract> findByEmployeeUserId(@Param("employeeUserId") Long employeeUserId);

    int updateToSent(
            @Param("id") Long id,
            @Param("ownerSignedAt") LocalDateTime ownerSignedAt,
            @Param("sentAt") LocalDateTime sentAt
    );

    int updateToSigned(
            @Param("id") Long id,
            @Param("employeeSignedAt") LocalDateTime employeeSignedAt
    );

    int updateToRejected(
            @Param("id") Long id,
            @Param("rejectedReason") String rejectedReason
    );

    int updateToCancelled(@Param("id") Long id);
}