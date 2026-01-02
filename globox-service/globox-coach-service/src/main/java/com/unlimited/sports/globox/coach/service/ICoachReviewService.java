package com.unlimited.sports.globox.coach.service;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.model.coach.dto.GetCoachReviewListDto;
import com.unlimited.sports.globox.model.coach.dto.GetCoachReviewRepliesDto;
import com.unlimited.sports.globox.model.coach.dto.PostCoachReplyDto;
import com.unlimited.sports.globox.model.coach.dto.PostCoachReviewDto;
import com.unlimited.sports.globox.model.coach.vo.CoachReviewVo;

/**
 * @since 2026/1/1 12:58
 * 教练评价服务接口
 */
public interface ICoachReviewService {

    /**
     * 获取教练的评价列表（一级评论）
     *
     * @param dto 查询条件
     * @return 分页后的评价列表
     */
    PaginationResult<CoachReviewVo> getCoachReviews(GetCoachReviewListDto dto);

    /**
     * 获取评价的回复列表
     *
     * @param dto 查询条件
     * @return 分页后的回复列表
     */
    PaginationResult<CoachReviewVo> getReviewReplies(GetCoachReviewRepliesDto dto);

    /**
     * 发布教练评价
     *
     * @param dto 评价内容
     * @return 评价ID
     */
    Long postReview(PostCoachReviewDto dto);

    /**
     * 教练回复评价
     *
     * @param dto 回复内容
     */
    Long replyReview(PostCoachReplyDto dto);
}
