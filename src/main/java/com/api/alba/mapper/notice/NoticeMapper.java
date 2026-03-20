package com.api.alba.mapper.notice;

import com.api.alba.domain.notice.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeMapper {
    int insert(Notice notice);

    Notice findById(@Param("id") Long id);

    List<Notice> findAll();

    int update(Notice notice);

    int delete(@Param("id") Long id);
}
