package com.api.alba.mapper.terms;

import com.api.alba.domain.terms.UserTermsAgreement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserTermsAgreementMapper {
    void insert(UserTermsAgreement agreement);

    List<UserTermsAgreement> findByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndTermsId(@Param("userId") Long userId, @Param("termsId") Long termsId);
}