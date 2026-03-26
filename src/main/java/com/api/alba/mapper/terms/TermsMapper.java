package com.api.alba.mapper.terms;

import com.api.alba.domain.terms.Terms;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TermsMapper {
    void insert(Terms terms);

    Terms findById(@Param("id") Long id);

    List<Terms> findActiveAll();

    Terms findActiveByType(@Param("termsType") String termsType);

    void deactivateByType(@Param("termsType") String termsType);
}