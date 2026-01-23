package com.unlimited.sports.globox.venue.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.VenueCode;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.dubbo.governance.SensitiveWordsDubboService;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoRequest;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoResponse;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueFacilityRelationMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.merchant.entity.VenueBusinessHours;
import com.unlimited.sports.globox.model.venue.dto.DeleteVenueReviewDto;
import com.unlimited.sports.globox.model.venue.dto.GetActivitiesByVenueDto;
import com.unlimited.sports.globox.model.venue.dto.GetVenueReviewListDto;
import com.unlimited.sports.globox.model.venue.dto.PostVenueReviewDto;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueFacilityRelation;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueReview;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivityParticipant;
import com.unlimited.sports.globox.model.venue.enums.*;
import com.unlimited.sports.globox.model.venue.enums.ReviewDeleteOperatorType;
import com.unlimited.sports.globox.venue.constants.ReviewConstants;
import com.unlimited.sports.globox.venue.mapper.venues.VenueReviewMapper;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplatePeriodMapper;
import com.unlimited.sports.globox.venue.mapper.VenueActivityMapper;
import com.unlimited.sports.globox.venue.mapper.VenueActivityParticipantMapper;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplatePeriod;
import com.unlimited.sports.globox.venue.constants.ActivityParticipantConstants;
import com.unlimited.sports.globox.venue.service.IVenueBusinessHoursService;
import com.unlimited.sports.globox.venue.service.IVenueService;
import com.unlimited.sports.globox.model.venue.vo.ActivityListVo;
import com.unlimited.sports.globox.model.venue.vo.VenueActivityDetailVo;
import com.unlimited.sports.globox.model.venue.vo.VenueDetailVo;
import com.unlimited.sports.globox.model.venue.vo.VenueDictItem;
import com.unlimited.sports.globox.model.venue.vo.VenueDictVo;
import com.unlimited.sports.globox.model.venue.vo.VenueReviewVo;
import com.unlimited.sports.globox.model.venue.vo.ActivityParticipantVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VenueServiceImpl implements IVenueService {

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private CourtMapper courtMapper;

    @Autowired
    private VenueReviewMapper venueReviewMapper;

    @Autowired
    private VenueActivityMapper venueActivityMapper;

    @Autowired
    private VenueActivityParticipantMapper venueActivityParticipantMapper;

    @Autowired
    private VenueFacilityRelationMapper venueFacilityRelationMapper;

    @Value("${default_image.venue_avatar}")
    private String defaultVenueUserAvatar;


    @DubboReference(group = "rpc")
    private SensitiveWordsDubboService sensitiveWordsDubboService;

    @DubboReference(group = "rpc")
    private UserDubboService userDubboService;

    @Autowired
    private IVenueBusinessHoursService venueBusinessHoursService;

    @Autowired
    private VenuePriceTemplatePeriodMapper venuePriceTemplatePeriodMapper;

    @Override
    public VenueDetailVo getVenueDetail(Long venueId) {
        Venue venue = venueMapper.selectById(venueId);
        if (venue == null) {
            throw new GloboxApplicationException("场馆不存在");
        }

        List<String> imageUrls = (venue.getImageUrls() != null && !venue.getImageUrls().isEmpty())
                ? Arrays.asList(venue.getImageUrls().split(";"))
                : Collections.emptyList();


        // 从关系表获取设施详情
        List<String> facilities = venueFacilityRelationMapper.selectList(
                new LambdaQueryWrapper<VenueFacilityRelation>()
                        .eq(VenueFacilityRelation::getVenueId, venueId)).stream().map(VenueFacilityRelation::getFacilityName).toList();


        List<Court> courts = courtMapper.selectList(
                new LambdaQueryWrapper<Court>().eq(Court::getVenueId, venueId)
        );

        List<Integer> courtTypes = courts.stream()
                .map(Court::getCourtType)
                .distinct()
                .toList();

        // 获取常规营业时间
        VenueBusinessHours businessHours = venueBusinessHoursService.getRegularBusinessHours(venueId);
        String defaultOpenTime = businessHours != null && businessHours.getOpenTime() != null
                ? businessHours.getOpenTime().toString()
                : "00:00";
        String defaultCloseTime = businessHours != null && businessHours.getCloseTime() != null ?
                businessHours.getCloseTime().toString()
                : "00:00";
        List<String> courtTypesDesc = courtTypes.stream()
                .map(type -> CourtType.fromValue(type).getDescription())
                .toList();

        // 动态计算最低价格
        BigDecimal minPrice = calculateMinPrice(venue.getTemplateId());

        return VenueDetailVo.builder()
                .venueId(venue.getVenueId())
                .name(venue.getName())
                .imageUrls(imageUrls)
                .avgRating(venue.getAvgRating())
                .ratingCount(venue.getRatingCount())
                .courtCount(courts.size())
                .courtTypes(courtTypes)
                .courtTypesDesc(courtTypesDesc)
                .address(venue.getAddress())
                .phone(venue.getPhone())
                .description(venue.getDescription())
                .facilities(facilities)
                .defaultOpenTime(defaultOpenTime)
                .defaultCloseTime(defaultCloseTime)
                .minPrice(minPrice)
                .build();
    }

    @Override
    public PaginationResult<VenueReviewVo> getVenueReviews(GetVenueReviewListDto dto) {
        // 查询一级评论（parent_review_id为null且review_type为USER_COMMENT的评论）
        Page<VenueReview> page = new Page<>(dto.getPage(), dto.getPageSize());
        Page<VenueReview> reviewPage = venueReviewMapper.selectPage(page,
                new LambdaQueryWrapper<VenueReview>()
                        .eq(VenueReview::getVenueId, dto.getVenueId())
                        .isNull(VenueReview::getParentReviewId)
                        .eq(VenueReview::getReviewType, ReviewType.USER_COMMENT.getValue())
                        .ne(VenueReview::getDeleted, true)
                        .orderByDesc(VenueReview::getCreatedAt)
        );
        if (reviewPage.getRecords().isEmpty()) {
            return PaginationResult.build(Collections.emptyList(), 0L, dto.getPage(), dto.getPageSize());
        }

        // 获取所有一级评论的ID
        List<Long> reviewIds = reviewPage.getRecords().stream()
                .map(VenueReview::getReviewId)
                .toList();


        /**
         * 目前不支持商家回复,后续可添加
         */
        // 查询每个一级评论的回复数量
        Map<Long, Integer> replyCountMap = getReplyCountMap(reviewIds);

        // 获取所有需要查询用户信息的用户ID（过滤掉匿名用户）
        List<Long> allUserIds = reviewPage.getRecords().stream()
                .filter(review -> review.getIsAnonymous() == null || !review.getIsAnonymous())
                .map(VenueReview::getUserId)
                .toList();

        BatchUserInfoRequest batchUserInfoRequest = new BatchUserInfoRequest();
        batchUserInfoRequest.setUserIds(allUserIds);
        // 只有当存在非匿名用户时，才发送 RPC 请求获取用户信息
        Map<Long, UserInfoVo> userInfoMap;
        if (!allUserIds.isEmpty()) {
            RpcResult<BatchUserInfoResponse> rpcResult = userDubboService.batchGetUserInfo(batchUserInfoRequest);
            Assert.rpcResultOk(rpcResult);
            BatchUserInfoResponse batchUserInfoResponse = rpcResult.getData();
            List<UserInfoVo> userInfoVOS = batchUserInfoResponse.getUsers();
            if (userInfoVOS != null && !userInfoVOS.isEmpty()) {
                userInfoMap = userInfoVOS.stream()
                        .collect(Collectors.toMap(UserInfoVo::getUserId, userInfo -> userInfo));
            } else {
                userInfoMap = new HashMap<>();
            }
        } else {
            userInfoMap = new HashMap<>();
        }

        // 构建返回结果
        List<VenueReviewVo> reviewVos = reviewPage.getRecords().stream()
                .map(review -> {
                    VenueReviewVo vo = transformToVo(review,userInfoMap);
                    vo.setReplyCount(replyCountMap.getOrDefault(review.getReviewId(), 0));
                    return vo;
                })
                .collect(Collectors.toList());
        return PaginationResult.build(reviewVos, reviewPage.getTotal(), dto.getPage(), dto.getPageSize());
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public void postReview(PostVenueReviewDto dto) {
        // 验证场馆是否存在
        Venue venue = venueMapper.selectById(dto.getVenueId());
        if (venue == null) {
            throw new GloboxApplicationException("场馆不存在");
        }
        // 敏感词校验
        RpcResult<Void> result = sensitiveWordsDubboService.checkSensitiveWords(dto.getContent());
        Assert.rpcResultOk(result);
        // todo 暂时没有回复功能,如果有,直接放开下面的代码 时间2025-12-23
//        // 如果是回复，验证父评论是否存在
//        if (dto.getParentReviewId() != null) {
//            VenueReview parentReview = venueReviewMapper.selectById(dto.getParentReviewId());
//            if (parentReview == null) {
//                throw new GloboxApplicationException("父评论不存在");
//            }
//        }

        // 处理图片URL列表
        String imageUrls = (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty())
                ? String.join(";", dto.getImageUrls())
                : null;

        // 构建评论实体
        VenueReview review = VenueReview.builder()
                .venueId(dto.getVenueId())
                .userId(dto.getUserId())
                .parentReviewId(dto.getParentReviewId())
                .reviewType(dto.getParentReviewId() == null ? ReviewType.USER_COMMENT.getValue() : ReviewType.MERCHANT_REPLY.getValue())
                .rating(dto.getRating())
                .content(dto.getContent())
                .isAnonymous(dto.getIsAnonymous() != null ? dto.getIsAnonymous() : false)
                .imageUrls(imageUrls)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 插入评论
        venueReviewMapper.insert(review);

        // 更新场馆的评分和评论数（仅一级评论才更新）
        if (dto.getParentReviewId() == null) {
            updateVenueRating(dto.getVenueId());
        }
    }


    // todo 通过定时任务更新?
    private void updateVenueRating(Long venueId) {
        // 查询所有一级评论
        List<VenueReview> reviews = venueReviewMapper.selectList(
                new LambdaQueryWrapper<VenueReview>()
                        .eq(VenueReview::getVenueId, venueId)
                        .isNull(VenueReview::getParentReviewId)
                        .eq(VenueReview::getReviewType, 1)
                        .ne(VenueReview::getDeleted, true)
        );

        if (!CollectionUtils.isEmpty(reviews)) {
            // 计算平均评分
            BigDecimal avgRating = reviews.stream()
                    .map(VenueReview::getRating)
                    .filter(Objects::nonNull)
                    .map(BigDecimal::valueOf)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(reviews.size()), 2, RoundingMode.HALF_UP);

            // 更新场馆评分
            Venue venue = venueMapper.selectById(venueId);
            if (venue != null) {
                venue.setAvgRating(avgRating);
                venue.setRatingCount(reviews.size());
                venueMapper.updateById(venue);
            }
        }
    }

    private VenueReviewVo transformToVo(VenueReview review, Map<Long, UserInfoVo> userInfoMap ) {
        // 解析图片URL列表
        List<String> imageUrls = (review.getImageUrls() != null && !review.getImageUrls().isEmpty())
                ? Arrays.asList(review.getImageUrls().split(";"))
                : Collections.emptyList();

        // 处理匿名用户
        String userName = null;
        String userAvatar = null;
        Long returnUserId = review.getUserId();

        if (review.getIsAnonymous() != null && review.getIsAnonymous()) {
            // 匿名用户
            userName = ReviewConstants.ANONYMOUS_USER_NAME;
            userAvatar = defaultVenueUserAvatar;
            returnUserId = -1L; // 匿名用户返回 -1L
        } else {
            // 非匿名用户，从 userInfoMap 获取用户信息
            UserInfoVo userInfoVO = userInfoMap.get(review.getUserId());
            userName = (userInfoVO != null && StringUtils.isNotBlank(userInfoVO.getNickName()))
                    ? userInfoVO.getNickName()
                    : ReviewConstants.UNKNOWN_USER;
            userAvatar = (userInfoVO != null && StringUtils.isNotBlank(userInfoVO.getAvatarUrl()))
                    ? userInfoVO.getAvatarUrl()
                    : defaultVenueUserAvatar;
        }

        return VenueReviewVo.builder()
                .reviewId(review.getReviewId())
                .userId(returnUserId)
                .userName(userName)
                .userAvatar(userAvatar)
                .rating(review.getRating())
                .content(review.getContent())
                .imageUrls(imageUrls)
                .createdAt(review.getCreatedAt())
                .build();
    }

    private Map<Long, Integer> getReplyCountMap(List<Long> reviewIds) {
        if (reviewIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 查询每个评论的回复数量
        List<VenueReview> allReplies = venueReviewMapper.selectList(
                new LambdaQueryWrapper<VenueReview>()
                        .in(VenueReview::getParentReviewId, reviewIds)
                        .eq(VenueReview::getReviewType, 2)
                        .ne(VenueReview::getDeleted, true)
        );

        // 按父评论ID分组统计数量
        return allReplies.stream()
                .collect(Collectors.groupingBy(
                        VenueReview::getParentReviewId,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
    }

    @Override
    public VenueDictVo getSearchFilterDictionary() {
        List<VenueDictItem> courtTypes = Arrays.stream(CourtType.values())
                .map(type -> VenueDictItem.builder()
                        .value(type.getValue())
                        .description(type.getDescription())
                        .build())
                .collect(Collectors.toList());

        List<VenueDictItem> groundTypes = Arrays.stream(GroundType.values())
                .map(type -> VenueDictItem.builder()
                        .value(type.getValue())
                        .description(type.getDescription())
                        .build())
                .collect(Collectors.toList());

        List<VenueDictItem> courtCountFilters = Arrays.stream(CourtCountFilter.values())
                .map(filter -> VenueDictItem.builder()
                        .value(filter.getValue())
                        .description(filter.getDescription())
                        .build())
                .collect(Collectors.toList());

        List<VenueDictItem> distances = Arrays.stream(DistanceFilter.values())
                .map(filter -> VenueDictItem.builder()
                        .value(filter.getValue())
                        .description(filter.getDescription())
                        .build())
                .collect(Collectors.toList());

        List<VenueDictItem> facilities = Arrays.stream(FacilityType.values())
                .map(facility -> VenueDictItem.builder()
                        .value(facility.getValue())
                        .description(facility.getDescription())
                        .build())
                .collect(Collectors.toList());

        return VenueDictVo.builder()
                .courtTypes(courtTypes)
                .groundTypes(groundTypes)
                .courtCountFilters(courtCountFilters)
                .distances(distances)
                .facilities(facilities)
                .build();
    }

    @Override
    public VenueActivityDetailVo getActivityDetail(Long activityId) {

        // 查询活动信息
        VenueActivity activity = venueActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new GloboxApplicationException(VenueCode.ACTIVITY_NOT_EXIST);
        }

        // 查询场地信息
        Court court = courtMapper.selectById(activity.getCourtId());
        if (court == null) {
            throw new GloboxApplicationException(VenueCode.COURT_NOT_EXIST);
        }

        // 查询场馆信息
        Venue venue = venueMapper.selectById(activity.getVenueId());
        if (venue == null) {
            throw new GloboxApplicationException(VenueCode.VENUE_NOT_EXIST);
        }

        // 查询活动参与者（只查询未取消的）
        List<VenueActivityParticipant> participants = venueActivityParticipantMapper.selectList(
                new LambdaQueryWrapper<VenueActivityParticipant>()
                        .eq(VenueActivityParticipant::getActivityId, activityId)
                        .eq(VenueActivityParticipant::getDeleteVersion, ActivityParticipantConstants.DELETE_VERSION_ACTIVE)
        );

        // 获取所有参与者的用户ID
        List<Long> userIds = participants.stream()
                .map(VenueActivityParticipant::getUserId)
                .toList();

        // 批量获取用户信息
        final Map<Long, UserInfoVo> userInfoMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            BatchUserInfoRequest batchUserInfoRequest = new BatchUserInfoRequest();
            batchUserInfoRequest.setUserIds(userIds);
            RpcResult<BatchUserInfoResponse> rpcResult = userDubboService.batchGetUserInfo(batchUserInfoRequest);
            Assert.rpcResultOk(rpcResult);
            BatchUserInfoResponse batchUserInfoResponse = rpcResult.getData();
            if (batchUserInfoResponse != null && batchUserInfoResponse.getUsers() != null) {
                userInfoMap.putAll(batchUserInfoResponse.getUsers().stream()
                        .collect(Collectors.toMap(UserInfoVo::getUserId, userInfo -> userInfo)));
            }
        }

        // 批量获取每个用户的参与活动次数
        final Map<Long, Integer> participationCountMap = getParticipationCountMap(userIds);

        // 构建参与者VO列表
        List<ActivityParticipantVo> participantVos = participants.stream()
                .map(participant -> {
                    UserInfoVo userInfo = userInfoMap.get(participant.getUserId());
                    int participationCount = participationCountMap.getOrDefault(participant.getUserId(), 0);

                    return ActivityParticipantVo.builder()
                            .userId(participant.getUserId())
                            .avatarUrl((userInfo != null && userInfo.getAvatarUrl() != null) ? userInfo.getAvatarUrl() : defaultVenueUserAvatar)
                            .nickName(userInfo != null ? userInfo.getNickName() : "")
                            .userNtrpLevel((userInfo != null && userInfo.getUserNtrpLevel() != null) ? userInfo.getUserNtrpLevel() : 0.0)
                            .participationCount(participationCount)
                            .build();
                })
                .collect(Collectors.toList());

        // 构建并返回活动详情
        return VenueActivityDetailVo.builder()
                .activityId(activity.getActivityId())
                .activityName(activity.getActivityName())
                .activityDate(activity.getActivityDate())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .courtName(court.getName())
                .venueName(venue.getName())
                .minNtrpLevel(activity.getMinNtrpLevel())
                .unitPrice(activity.getUnitPrice())
                .currentParticipants(activity.getCurrentParticipants())
                .maxParticipants(activity.getMaxParticipants())
                .participants(participantVos)
                .status(activity.getStatus())
                .build();
    }

    /**
     * 批量获取用户的参与活动次数
     */
    private Map<Long, Integer> getParticipationCountMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        // 查询所有未取消的参与记录
        List<VenueActivityParticipant> participants = venueActivityParticipantMapper.selectList(
                new LambdaQueryWrapper<VenueActivityParticipant>()
                        .in(VenueActivityParticipant::getUserId, userIds)
                        .eq(VenueActivityParticipant::getDeleteVersion, ActivityParticipantConstants.DELETE_VERSION_ACTIVE)
        );

        // 按userId分组统计count
        Map<Long, Integer> participationCountMap = new HashMap<>();
        for (VenueActivityParticipant participant : participants) {
            Long userId = participant.getUserId();
            participationCountMap.put(userId, participationCountMap.getOrDefault(userId, 0) + 1);
        }

        return participationCountMap;
    }

    /**
     * 动态计算场馆最低价格
     * 从价格模板中获取所有价格类型（工作日、周末、节假日），取最小值
     * 如果查不到价格则默认999
     *
     * @param templateId 价格模板ID
     * @return 最低价格
     */
    private BigDecimal calculateMinPrice(Long templateId) {
        if (templateId == null) {
            return new BigDecimal("999");
        }

        List<VenuePriceTemplatePeriod> periods = venuePriceTemplatePeriodMapper.selectList(
                new LambdaQueryWrapper<VenuePriceTemplatePeriod>()
                        .eq(VenuePriceTemplatePeriod::getTemplateId, templateId)
                        .eq(VenuePriceTemplatePeriod::getIsEnabled, 1)
        );

        if (periods == null || periods.isEmpty()) {
            return new BigDecimal("999");
        }

        return periods.stream()
                .flatMap(period -> java.util.Arrays.stream(
                        new BigDecimal[]{period.getWeekdayPrice(), period.getWeekendPrice(), period.getHolidayPrice()}
                ))
                .filter(price -> price != null)
                .min(BigDecimal::compareTo)
                .orElse(new BigDecimal("999"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(DeleteVenueReviewDto dto) {
        // 1. 查询评论是否存在
        VenueReview review = venueReviewMapper.selectById(dto.getReviewId());
        if (review == null) {
            throw new GloboxApplicationException("评论不存在");
        }

        // 2. 验证是否已删除
        if (review.getDeleted() != null && review.getDeleted()) {
            throw new GloboxApplicationException("评论已被删除");
        }

        // 3. 验证权限：只能删除自己的评论
        if (!review.getUserId().equals(dto.getUserId())) {
            throw new GloboxApplicationException("无权删除他人评论");
        }

        // 4. 执行软删除 - 使用 LambdaUpdateWrapper 显式更新字段
        LocalDateTime now = LocalDateTime.now();
        int deleteOperatorType = dto.getDeleteOperatorType() != null ?
                dto.getDeleteOperatorType() : ReviewDeleteOperatorType.USER_SELF.getValue();

        int updateCount = venueReviewMapper.update(null,
                new LambdaUpdateWrapper<VenueReview>()
                        .eq(VenueReview::getReviewId, dto.getReviewId())
                        .set(VenueReview::getDeleted, true)
                        .set(VenueReview::getDeletedAt, now)
                        .set(VenueReview::getDeleteOperatorType, deleteOperatorType)
                        .set(VenueReview::getUpdatedAt, now)
        );

        log.info("删除场馆评论 reviewId={}, deleteOperatorType={}, updateCount={}",
                dto.getReviewId(), deleteOperatorType, updateCount);

        // 5. 如果是一级评论，需要重新计算场馆评分
        if (review.getParentReviewId() == null &&
                review.getReviewType().equals(ReviewType.USER_COMMENT.getValue())) {
            updateVenueRating(review.getVenueId());
        }
    }

    @Override
    public List<ActivityListVo> getVenueActivityList(GetActivitiesByVenueDto dto) {
        // 查询指定场馆和日期的所有正常活动
        List<VenueActivity> activities = venueActivityMapper.selectList(
                new LambdaQueryWrapper<VenueActivity>()
                        .eq(VenueActivity::getVenueId, dto.getVenueId())
                        .eq(VenueActivity::getActivityDate, dto.getActivityDate())
                        .eq(VenueActivity::getStatus, VenueActivityStatusEnum.NORMAL.getValue())
                        .orderByAsc(VenueActivity::getStartTime)
        );

        if (activities.isEmpty()) {
            return Collections.emptyList();
        }

        // 转换为VO列表
        return activities.stream()
                .map(activity -> {
                    // 解析图片URL列表
                    List<String> imageUrls = (activity.getImageUrls() != null && !activity.getImageUrls().isEmpty())
                            ? activity.getImageUrls()
                            : Collections.emptyList();

                    return ActivityListVo.builder()
                            .activityId(activity.getActivityId())
                            .venueId(activity.getVenueId())
                            .courtId(activity.getCourtId())
                            .activityTypeId(activity.getActivityTypeId())
                            .activityTypeDesc(activity.getActivityTypeDesc())
                            .activityName(activity.getActivityName())
                            .imageUrls(imageUrls)
                            .activityDate(activity.getActivityDate())
                            .startTime(activity.getStartTime())
                            .endTime(activity.getEndTime())
                            .maxParticipants(activity.getMaxParticipants())
                            .currentParticipants(activity.getCurrentParticipants())
                            .unitPrice(activity.getUnitPrice())
                            .description(activity.getDescription())
                            .registrationDeadline(activity.getRegistrationDeadline())
                            .minNtrpLevel(activity.getMinNtrpLevel())
                            .build();
                })
                .collect(Collectors.toList());
    }

}
