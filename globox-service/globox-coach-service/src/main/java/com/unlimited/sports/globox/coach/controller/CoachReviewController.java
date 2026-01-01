package com.unlimited.sports.globox.coach.controller;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.unlimited.sports.globox.coach.service.ICoachReviewService;
import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.coach.dto.GetCoachReviewListDto;
import com.unlimited.sports.globox.model.coach.dto.GetCoachReviewRepliesDto;
import com.unlimited.sports.globox.model.coach.dto.PostCoachReviewDto;
import com.unlimited.sports.globox.model.coach.vo.CoachReviewVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * @since 2026/1/1 13:15
 * 教练评价Controller
 */
@Slf4j
@RestController
@RequestMapping("/coach/reviews")
public class CoachReviewController {

    @Autowired
    private ICoachReviewService coachReviewService;

    /**
     * 获取教练的评价列表
     *
     * @param coachUserId 教练用户ID
     * @param dto 查询条件
     * @return 分页的评价列表
     */
    @GetMapping("/coaches/{coachUserId}")
    public R<PaginationResult<CoachReviewVo>> getCoachReviews(
            @PathVariable Long coachUserId,
            @Valid GetCoachReviewListDto dto) {
        dto.setCoachUserId(coachUserId);
        PaginationResult<CoachReviewVo> result = coachReviewService.getCoachReviews(dto);
        return R.ok(result);
    }

    /**
     * 获取评价的回复列表
     *
     * @param reviewId 评价ID
     * @param dto 查询条件
     * @return 分页的回复列表
     */
    @GetMapping("/{reviewId}/replies")
    public R<PaginationResult<CoachReviewVo>> getReviewReplies(
            @PathVariable Long reviewId,
            @Valid GetCoachReviewRepliesDto dto) {
        dto.setParentReviewId(reviewId);
        PaginationResult<CoachReviewVo> result = coachReviewService.getReviewReplies(dto);
        return R.ok(result);
    }

    /**
     * 发布教练评价（学员评价）
     *
     * @param dto 评价内容
     * @param request HTTP请求
     * @return 成功标识
     */
    @PostMapping
    public R<Void> postReview(@Valid @RequestBody PostCoachReviewDto dto,
                              HttpServletRequest request) {
        String userIdStr = request.getHeader(RequestHeaderConstants.HEADER_USER_ID);
        if (StringUtils.isBlank(userIdStr)) {
            throw new GloboxApplicationException("未获取到用户ID，无法发布评价");
        }

        long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new GloboxApplicationException("X-User-Id必须为数字");
        }

        dto.setUserId(userId);
        coachReviewService.postReview(dto);
        return R.ok(null);
    }

    /**
     * 教练回复评价
     *
     * @param reviewId 评价ID
     * @param dto 回复内容
     * @param request HTTP请求
     * @return 成功标识
     */
    @PostMapping("/{reviewId}/reply")
    public R<Void> replyReview(@PathVariable Long reviewId,
                               @Valid @RequestBody PostCoachReviewDto dto,
                               HttpServletRequest request) {
        String userIdStr = request.getHeader(RequestHeaderConstants.HEADER_USER_ID);
        if (StringUtils.isBlank(userIdStr)) {
            throw new GloboxApplicationException("未获取到用户ID，无法发布回复");
        }

        long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new GloboxApplicationException("X-User-Id必须为数字");
        }

        dto.setUserId(userId);
        dto.setParentReviewId(reviewId);
        coachReviewService.replyReview(dto);
        return R.ok(null);
    }
}
