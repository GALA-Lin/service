package com.unlimited.sports.globox.coach.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.coach.mapper.CoachProfileMapper;
import com.unlimited.sports.globox.coach.mapper.CoachReviewsMapper;
import com.unlimited.sports.globox.coach.service.ICoachReviewService;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoRequest;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoResponse;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.model.coach.dto.GetCoachReviewListDto;
import com.unlimited.sports.globox.model.coach.dto.GetCoachReviewRepliesDto;
import com.unlimited.sports.globox.model.coach.dto.PostCoachReplyDto;
import com.unlimited.sports.globox.model.coach.dto.PostCoachReviewDto;
import com.unlimited.sports.globox.model.coach.entity.CoachProfile;
import com.unlimited.sports.globox.model.coach.entity.CoachReviews;
import com.unlimited.sports.globox.model.coach.enums.ReviewTypeEnum;
import com.unlimited.sports.globox.model.coach.vo.CoachReviewVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.unlimited.sports.globox.model.coach.CoachReviewConstants.*;

/**
 * @since 2026/1/1 13:00
 *
 */
@Slf4j
@Service
public class CoachReviewServiceImpl implements ICoachReviewService {

    @Autowired
    private CoachReviewsMapper coachReviewsMapper;

    @Autowired
    private CoachProfileMapper coachProfileMapper;

    @DubboReference(group = "rpc")
    private UserDubboService userDubboService;



    /**
     * 获取教练的评价列表（一级评论）
     *
     * @param dto 查询条件
     * @return 分页后的评价列表
     */
    @Override
    public PaginationResult<CoachReviewVo> getCoachReviews(GetCoachReviewListDto dto) {
        log.info("获取教练评价列表 - coachUserId: {}, page: {}/{}",
                dto.getCoachUserId(), dto.getPage(), dto.getPageSize());

        // 构建查询条件
        LambdaQueryWrapper<CoachReviews> wrapper = new LambdaQueryWrapper<CoachReviews>()
                .eq(CoachReviews::getCoachUserId, dto.getCoachUserId())
                .isNull(CoachReviews::getParentReviewId)  // 只查询一级评论
                .eq(CoachReviews::getReviewType, ReviewTypeEnum.USER_COMMENT.getValue())
                .eq(CoachReviews::getReviewStatus, 1)  // 只显示正常状态的评价
                .orderByDesc(CoachReviews::getCreatedAt);

        // 评分筛选
        if (dto.getRatingFilter() != null) {
            wrapper.eq(CoachReviews::getOverallRating, dto.getRatingFilter());
        }

        // 只看有图评价
        if (Boolean.TRUE.equals(dto.getWithImagesOnly())) {
            wrapper.isNotNull(CoachReviews::getReviewImages);
        }

        // 分页查询
        Page<CoachReviews> page = new Page<>(dto.getPage(), dto.getPageSize());
        Page<CoachReviews> reviewPage = coachReviewsMapper.selectPage(page, wrapper);

        if (reviewPage.getRecords().isEmpty()) {
            return PaginationResult.build(Collections.emptyList(), 0L, dto.getPage(), dto.getPageSize());
        }

        // 获取评论ID列表
        List<Long> reviewIds = reviewPage.getRecords().stream()
                .map(CoachReviews::getCoachReviewsId)
                .collect(Collectors.toList());

        // 批量查询回复数量
        Map<Long, Integer> replyCountMap = getReplyCountMap(reviewIds);

        // 获取所有需要查询用户信息的用户ID
        List<Long> allUserIds = reviewPage.getRecords().stream()
                .filter(review -> review.getIsAnonymous() == null || review.getIsAnonymous() == 0)
                .map(CoachReviews::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 批量获取用户信息
        Map<Long, UserInfoVo> userInfoMap = batchGetUserInfo(allUserIds);

        // 查询每个评价的教练回复（如果有）
        Map<Long, CoachReviews> coachReplyMap = getCoachReplyMap(reviewIds);

        // 获取教练用户信息（用于显示教练回复）
        Set<Long> coachUserIds = coachReplyMap.values().stream()
                .map(CoachReviews::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserInfoVo> coachInfoMap = batchGetUserInfo(new ArrayList<>(coachUserIds));

        // 构建返回结果
        List<CoachReviewVo> reviewVos = reviewPage.getRecords().stream()
                .map(review -> buildReviewVo(review, userInfoMap, replyCountMap, coachReplyMap, coachInfoMap))
                .collect(Collectors.toList());

        return PaginationResult.build(reviewVos, reviewPage.getTotal(), dto.getPage(), dto.getPageSize());
    }
    /**
     * 获取评价的回复列表
     *
     * @param dto 查询条件
     * @return 分页后的回复列表
     */
    @Override
    public PaginationResult<CoachReviewVo> getReviewReplies(GetCoachReviewRepliesDto dto) {
        log.info("获取评价回复列表 - parentReviewId: {}, page: {}/{}",
                dto.getParentReviewId(), dto.getPage(), dto.getPageSize());

        // 分页查询回复
        Page<CoachReviews> page = new Page<>(dto.getPage(), dto.getPageSize());
        Page<CoachReviews> replyPage = coachReviewsMapper.selectPage(page,
                new LambdaQueryWrapper<CoachReviews>()
                        .eq(CoachReviews::getParentReviewId, dto.getParentReviewId())
                        .eq(CoachReviews::getReviewStatus, 1)
                        .orderByAsc(CoachReviews::getCreatedAt)
        );

        if (replyPage.getRecords().isEmpty()) {
            return PaginationResult.build(Collections.emptyList(), 0L, dto.getPage(), dto.getPageSize());
        }

        // 获取所有回复用户ID
        List<Long> userIds = replyPage.getRecords().stream()
                .map(CoachReviews::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 批量获取用户信息
        Map<Long, UserInfoVo> userInfoMap = batchGetUserInfo(userIds);

        // 构建返回结果
        List<CoachReviewVo> replyVos = replyPage.getRecords().stream()
                .map(reply -> buildReviewVo(reply, userInfoMap, Collections.emptyMap(),
                        Collections.emptyMap(), Collections.emptyMap()))
                .collect(Collectors.toList());

        return PaginationResult.build(replyVos, replyPage.getTotal(), dto.getPage(), dto.getPageSize());
    }
    /**
     * 发布教练评价
     *
     * @param dto 评价内容
     * @return 评价ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long postReview(PostCoachReviewDto dto) {
        log.info("发布教练评价 - userId: {}, coachUserId: {}, bookingId: {}",
                dto.getUserId(), dto.getCoachUserId(), dto.getCoachBookingId());

        // 验证教练是否存在
        CoachProfile coachProfile = coachProfileMapper.selectOne(
                new LambdaQueryWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, dto.getCoachUserId())
        );
        if (coachProfile == null) {
            throw new GloboxApplicationException("教练不存在");
        }

        // 检查是否已经评价过
        Long existingCount = coachReviewsMapper.selectCount(
                new LambdaQueryWrapper<CoachReviews>()
                        .eq(CoachReviews::getCoachBookingId, dto.getCoachBookingId())
                        .eq(CoachReviews::getUserId, dto.getUserId())
                        .eq(CoachReviews::getReviewType, ReviewTypeEnum.USER_COMMENT.getValue())
        );
        if (existingCount > 0) {
            throw new GloboxApplicationException("您已经评价过该订单");
        }

        // 构建评价实体
        CoachReviews review = CoachReviews.builder()
                .coachBookingId(dto.getCoachBookingId())
                .userId(dto.getUserId())
                .coachUserId(dto.getCoachUserId())
                .parentReviewId(null)
                .reviewType(ReviewTypeEnum.USER_COMMENT.getValue())
                .overallRating(dto.getOverallRating())
                .professionalismRating(dto.getProfessionalismRating())
                .teachingRating(dto.getTeachingRating())
                .attitudeRating(dto.getAttitudeRating())
                .reviewContent(dto.getReviewContent())
                .reviewImages(dto.getReviewImages())
                .reviewTags(dto.getReviewTags())
                .isAnonymous(dto.getIsAnonymous() ? 1 : 0)
                .reviewStatus(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 插入评论
        coachReviewsMapper.insert(review);

        // 更新教练的评分和评价数
        updateCoachRating(dto.getCoachUserId());

        log.info("评价发布成功 - reviewId: {}", review.getCoachReviewsId());
        return  review.getCoachReviewsId();
    }
    /**
     * 教练回复评价
     *
     * @param dto 回复内容
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long replyReview(PostCoachReplyDto dto) {
        log.info("教练回复评价 - coachUserId: {}, parentReviewId: {}",
                dto.getCoachUserId(), dto.getParentReviewId());

        // 验证父评论是否存在
        CoachReviews parentReview = coachReviewsMapper.selectById(dto.getParentReviewId());
        if (parentReview == null) {
            throw new GloboxApplicationException("父评论不存在");
        }

        // 验证是否是该教练的评价
        if (!parentReview.getCoachUserId().equals(dto.getCoachUserId())) {
            throw new GloboxApplicationException("只能回复自己的评价");
        }

        // 检查是否已经回复过
        Long existingCount = coachReviewsMapper.selectCount(
                new LambdaQueryWrapper<CoachReviews>()
                        .eq(CoachReviews::getParentReviewId, dto.getParentReviewId())
                        .eq(CoachReviews::getReviewType, ReviewTypeEnum.COACH_REPLY.getValue())
        );
        if (existingCount > 0) {
            throw new GloboxApplicationException("您已经回复过该评价");
        }

        // 构建回复实体
        CoachReviews reply = CoachReviews.builder()
                .coachBookingId(parentReview.getCoachBookingId())
                .userId(dto.getUserId())  // 教练的用户ID
                .coachUserId(dto.getCoachUserId())
                .parentReviewId(dto.getParentReviewId())
                .reviewType(ReviewTypeEnum.COACH_REPLY.getValue())
                // 教练回复不需要评分字段，全部设为 null
                .overallRating(null)
                .professionalismRating(null)
                .teachingRating(null)
                .attitudeRating(null)
                .reviewContent(dto.getReplyContent())
                .reviewImages(null)  // 教练回复不需要图片
                .reviewTags(null)    // 教练回复不需要标签
                .isAnonymous(0)
                .reviewStatus(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 插入回复
        coachReviewsMapper.insert(reply);

        log.info("回复发布成功 - replyId: {}", reply.getCoachReviewsId());
        return reply.getCoachReviewsId();
    }

    /**
     * 更新教练的评分和评价数
     */
    private void updateCoachRating(Long coachUserId) {
        // 查询所有一级评价
        List<CoachReviews> reviews = coachReviewsMapper.selectList(
                new LambdaQueryWrapper<CoachReviews>()
                        .eq(CoachReviews::getCoachUserId, coachUserId)
                        .isNull(CoachReviews::getParentReviewId)
                        .eq(CoachReviews::getReviewType, ReviewTypeEnum.USER_COMMENT.getValue())
                        .eq(CoachReviews::getReviewStatus, 1)
        );

        if (reviews.isEmpty()) {
            return;
        }

        // 计算平均评分
        BigDecimal avgRating = reviews.stream()
                .map(CoachReviews::getOverallRating)
                .filter(Objects::nonNull)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(reviews.size()), 2, RoundingMode.HALF_UP);

        // 更新教练评分
        CoachProfile coachProfile = new CoachProfile();
        coachProfile.setCoachUserId(coachUserId);
        coachProfile.setCoachRatingScore(avgRating);
        coachProfile.setCoachRatingCount(reviews.size());

        coachProfileMapper.update(coachProfile,
                new LambdaQueryWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, coachUserId)
        );

        log.info("教练评分更新成功 - coachUserId: {}, avgRating: {}, count: {}",
                coachUserId, avgRating, reviews.size());
    }

    /**
     * 批量获取用户信息
     */
    private Map<Long, UserInfoVo> batchGetUserInfo(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        BatchUserInfoRequest request = new BatchUserInfoRequest();
        request.setUserIds(userIds);
        BatchUserInfoResponse response = userDubboService.batchGetUserInfo(request);

        if (response == null || response.getUsers() == null) {
            return Collections.emptyMap();
        }

        return response.getUsers().stream()
                .collect(Collectors.toMap(UserInfoVo::getUserId, userInfo -> userInfo));
    }

    /**
     * 获取回复数量映射
     */
    private Map<Long, Integer> getReplyCountMap(List<Long> reviewIds) {
        if (reviewIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Map<String, Object>> replyCounts = coachReviewsMapper.selectReplyCountByReviewIds(reviewIds);

        return replyCounts.stream()
                .collect(Collectors.toMap(
                        map -> ((Number) map.get("reviewId")).longValue(),
                        map -> ((Number) map.get("replyCount")).intValue()
                ));
    }

    /**
     * 获取教练回复映射（每个评价只取第一条教练回复）
     */
    private Map<Long, CoachReviews> getCoachReplyMap(List<Long> reviewIds) {
        if (reviewIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<CoachReviews> replies = coachReviewsMapper.selectList(
                new LambdaQueryWrapper<CoachReviews>()
                        .in(CoachReviews::getParentReviewId, reviewIds)
                        .eq(CoachReviews::getReviewType, ReviewTypeEnum.COACH_REPLY.getValue())
                        .eq(CoachReviews::getReviewStatus, 1)
                        .orderByAsc(CoachReviews::getCreatedAt)
        );

        // 每个父评论只取第一条回复
        return replies.stream()
                .collect(Collectors.toMap(
                        CoachReviews::getParentReviewId,
                        reply -> reply,
                        (existing, replacement) -> existing  // 保留第一条
                ));
    }

    /**
     * 构建评价VO
     */
    private CoachReviewVo buildReviewVo(CoachReviews review,
                                        Map<Long, UserInfoVo> userInfoMap,
                                        Map<Long, Integer> replyCountMap,
                                        Map<Long, CoachReviews> coachReplyMap,
                                        Map<Long, UserInfoVo> coachInfoMap) {
        // 直接使用 List，不需要解析
        List<String> imageUrls = review.getReviewImages() != null ?
                review.getReviewImages() : Collections.emptyList();

        List<String> tags = review.getReviewTags() != null ?
                review.getReviewTags() : Collections.emptyList();

        // 处理用户信息
        String userName;
        String userAvatar;
        Long returnUserId = review.getUserId();
        Double userNtrpLevel = null;
        String ntrpLevelDesc = null;

        String defaultCoachReviewUserAvatar = DEFAULT_CoachReviewUserAvatar;
        if (review.getIsAnonymous() != null && review.getIsAnonymous() == 1) {
            userName = ANONYMOUS_USER_NAME;
            userAvatar = defaultCoachReviewUserAvatar;
            returnUserId = -1L;
        } else {
            UserInfoVo userInfo = userInfoMap.get(review.getUserId());
            userName = (userInfo != null && StringUtils.isNotBlank(userInfo.getNickName()))
                    ? userInfo.getNickName()
                    : UNKNOWN_USER;
            userAvatar = (userInfo != null && StringUtils.isNotBlank(userInfo.getAvatarUrl()))
                    ? userInfo.getAvatarUrl()
                    : defaultCoachReviewUserAvatar;
        }

        // 处理教练回复
        CoachReviewVo.CoachReplyVo coachReplyVo = null;
        CoachReviews coachReply = coachReplyMap.get(review.getCoachReviewsId());
        if (coachReply != null) {
            UserInfoVo coachInfo = coachInfoMap.get(coachReply.getUserId());
            coachReplyVo = CoachReviewVo.CoachReplyVo.builder()
                    .replyId(coachReply.getCoachReviewsId())
                    .coachUserId(coachReply.getUserId())
                    .coachName(coachInfo != null ? coachInfo.getNickName() : "教练")
                    .coachAvatar(coachInfo != null ? coachInfo.getAvatarUrl() : defaultCoachReviewUserAvatar)
                    .replyContent(coachReply.getReviewContent())
                    .replyTime(coachReply.getCreatedAt())
                    .build();
        }

        return CoachReviewVo.builder()
                .reviewId(review.getCoachReviewsId())
                .userId(returnUserId)
                .userName(userName)
                .userAvatar(userAvatar)
                .userNtrpLevel(userNtrpLevel)
                .ntrpLevelDesc(ntrpLevelDesc)
                .overallRating(review.getOverallRating())
                .professionalismRating(review.getProfessionalismRating())
                .teachingRating(review.getTeachingRating())
                .attitudeRating(review.getAttitudeRating())
                .reviewContent(review.getReviewContent())
                .reviewImages(imageUrls)
                .reviewTags(tags)
                .isAnonymous(review.getIsAnonymous() == 1)
                .replyCount(replyCountMap.getOrDefault(review.getCoachReviewsId(), 0))
                .createdAt(review.getCreatedAt())
                .coachReply(coachReplyVo)
                .build();
    }
}
