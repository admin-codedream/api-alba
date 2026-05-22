package com.api.alba.mapper.notice;

import com.api.alba.domain.notice.WorkplaceNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkplaceNoticeMapper {

    int insert(WorkplaceNotice notice);

    WorkplaceNotice findById(@Param("id") Long id);

    List<WorkplaceNotice> findAllByWorkplaceId(@Param("workplaceId") Long workplaceId);

    int update(
            @Param("id") Long id,
            @Param("title") String title,
            @Param("content") String content
    );

    int updatePinned(
            @Param("id") Long id,
            @Param("pinned") boolean pinned
    );

    int deleteById(@Param("id") Long id);
}