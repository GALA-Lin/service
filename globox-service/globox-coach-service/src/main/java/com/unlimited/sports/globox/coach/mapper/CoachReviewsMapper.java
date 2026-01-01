package com.unlimited.sports.globox.coach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.coach.entity.CoachReviews;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @since 2026/1/1 13:01
 *
 */
@Mapper
public interface CoachReviewsMapper extends BaseMapper<CoachReviews> {

    /**
     * 查询评论的回复数量（用于批量查询）
     *
     * @param reviewIds 评论ID列表
     * @return 评论ID -> 回复数量的映射
     */
    List<java.util.Map<String, Object>> selectReplyCountByReviewIds(@Param("reviewIds") List<Long> reviewIds);
}
