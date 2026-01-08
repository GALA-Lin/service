package com.unlimited.sports.globox.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.model.social.dto.RallyPostsDto;
import com.unlimited.sports.globox.model.social.dto.RallyQueryDto;
import com.unlimited.sports.globox.model.social.dto.UpdateRallyDto;
import com.unlimited.sports.globox.model.social.entity.*;
import com.unlimited.sports.globox.model.social.vo.*;
import com.unlimited.sports.globox.model.venue.enums.GroundType;
import com.unlimited.sports.globox.social.mapper.FilterRegionMapper;
import com.unlimited.sports.globox.social.mapper.RallyApplicationMapper;
import com.unlimited.sports.globox.social.mapper.RallyParticipantMapper;
import com.unlimited.sports.globox.social.mapper.RallyPostsMapper;
import com.unlimited.sports.globox.social.service.RallyService;
import java.util.LinkedHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.unlimited.sports.globox.model.social.vo.RallyQueryVo.DictItem;

/**
 * 社交活动服务实现类
 * 实现了RallyService接口，提供社交活动相关的业务逻辑处理
 * 包括活动创建、查询、参与、审核等功能
 **/
@Service
@Slf4j
public class RallyServiceImpl implements RallyService {

    @Autowired
    private RallyPostsMapper rallyPostsMapper;

    @Autowired
    private RallyParticipantMapper rallyParticipantMapper;

    @Autowired
    private RallyApplicationMapper rallyApplicationMapper;

    @Autowired
    private FilterRegionMapper filterRegionMapper;

    @DubboReference(group = "rpc")
    private UserDubboService userDubboService;

    /**
     * 获取社交活动列表
     * @param rallyQueryDto 查询条件
     * @return 分页结果
     */
    @Override
    public PaginationResult<RallyPostsVo> getRallyPostsList(RallyQueryDto rallyQueryDto, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        log.info("获取社交活动列表 - rallyQueryDto: ·················{}", rallyQueryDto);
        List<RallyPosts> rallyPostsList = rallyPostsMapper.getRallyPostsList(rallyQueryDto.getArea(), rallyQueryDto.getTimeRange(), rallyQueryDto.getGenderLimit(),rallyQueryDto.getNtrpMin(),rallyQueryDto.getNtrpMax(),rallyQueryDto.getActivityType(), offset, pageSize);
        log.info("获取社交活动列表成功 - rallyPostsList: ========================={}", rallyPostsList);
        List<RallyPostsVo> rallyPostsVos = rallyPostsToRallyPostsVo(rallyPostsList);
        log.info("获取社交活动列表成功 - rallyPostsVos: ------------------------{}", rallyPostsVos);
        Long count = rallyPostsMapper.countRallyPostsList(rallyQueryDto.getArea(), rallyQueryDto.getTimeRange(), rallyQueryDto.getGenderLimit(),rallyQueryDto.getNtrpMin(),rallyQueryDto.getNtrpMax(),rallyQueryDto.getActivityType());
        PaginationResult<RallyPostsVo> rallyList = PaginationResult.build(
                rallyPostsVos,
                count,
                page,
                pageSize);
        return rallyList;
    }

    /**
     * 获取社交活动详情
     * @param postId 活动ID
     * @param rallyApplicantId 申请人ID
     * @return 活动详情
     */
    @Override
    public RallyPostsDetailsVo getRallyDetails(Long postId, Long rallyApplicantId) {
        RallyPosts rallyPosts = rallyPostsMapper.selectByPostId(postId);
        int rallyApplicationStatus = -1;
        boolean isOwner = false;
        if (rallyPosts == null) {
            return null;
        }
        if (rallyApplicantId.equals(rallyPosts.getInitiatorId())){
            isOwner = true;
        }
        RallyApplication rallyApplication = rallyApplicationMapper.selectByRallyIdAndApplicantId(rallyPosts.getRallyPostId(), rallyApplicantId);
        if (rallyApplication == null) {
            rallyApplicationStatus = RallyApplyStatusEnum.DEFAULT.getCode();
        }else {
            rallyApplicationStatus = rallyApplication.getStatus();
        }
        List<RallyParticipantVo> rallyParticipantVos = rallyParticipantVos(rallyPosts.getRallyPostId());
        log.info("查询社交活动详情成功 - rallyPosts: {}, rallyParticipantsVoList: {}", rallyPosts, rallyParticipantVos);
        RallyPostsDetailsVo rallyPostsDetailsVo = RallyPostsDetailsVo.builder()
                .rallyPostId(rallyPosts.getRallyPostId())
                .rallyInitiatorId(rallyPosts.getInitiatorId())
                .rallyTitle(rallyPosts.getRallyTitle())
                .rallyRegion(rallyPosts.getRallyRegion())
                .rallyVenueName(rallyPosts.getRallyVenueName())
                .rallyCourtName(rallyPosts.getRallyCourtName())
                .rallyParticipants(rallyParticipantVos)
                .rallyEventDate(rallyPosts.getRallyEventDate())
                .rallyStartTime(rallyPosts.getRallyStartTime())
                .rallyEndTime(rallyPosts.getRallyEndTime())
                .rallyGenderLimit(rallyPosts.getRallyGenderLimit())
                .ntrpMin(rallyPosts.getRallyNtrpMin())
                .ntrpMax(rallyPosts.getRallyNtrpMax())
                .rallyTotalPeople(rallyPosts.getRallyTotalPeople())
                .rallyRemainingPeople(rallyPosts.getRallyRemainingPeople())
                .rallyStatus(rallyPosts.getRallyStatus())
                .rallyLabel(getRallyLabel(rallyPosts))
                .rallyCost(rallyPosts.getRallyCost())
                .rallyCostBearer(RallyCostBearerEnum.getDescByCode(rallyPosts.getRallyCostBearer()))
                .rallyApplicationStatus(rallyApplicationStatus)
                .isOwner(isOwner)
                .build();
        log.info("查询社交活动详情成功 - rallyPostsDetailsVo: {}", rallyPostsDetailsVo);
        return rallyPostsDetailsVo;
    }

    /**
     * 创建社交活动
     * @param rallyPostsDto 活动DTO
     * @param rallyApplicantId 申请人ID
     * @return 创建的活动实体
     */
    @Override
    @Transactional
    public RallyPosts createRally(RallyPostsDto rallyPostsDto, Long rallyApplicantId) {
        log.info("创建社交活动 - rallyPostsDto: {}, rallyApplicantId: {}", rallyPostsDto, rallyApplicantId);
        if (rallyPostsDto.getRallyEventDate().isBefore(LocalDate.now())){
            return null;
        }
        RallyPosts rallyPosts = RallyPosts.builder()
                .initiatorId(rallyApplicantId)
                .rallyTitle(rallyPostsDto.getRallyTitle())
                .rallyRegion(rallyPostsDto.getRallyRegion())
                .rallyVenueName(rallyPostsDto.getRallyVenueName())
                .rallyCourtName(rallyPostsDto.getRallyCourtName())
                .rallyEventDate(rallyPostsDto.getRallyEventDate())
                .rallyTimeType(rallyPostsDto.getRallyTimeType())
                .rallyStartTime(rallyPostsDto.getRallyStartTime())
                .rallyEndTime(rallyPostsDto.getRallyEndTime())
                .rallyCost(rallyPostsDto.getRallyCost())
                .rallyCostBearer(rallyPostsDto.getRallyCostBearer())
                .rallyActivityType(rallyPostsDto.getRallyActivityType())
                .rallyGenderLimit(rallyPostsDto.getRallyGenderLimit())
                .rallyNtrpMin(rallyPostsDto.getRallyNtrpMin())
                .rallyNtrpMax(rallyPostsDto.getRallyNtrpMax())
                .rallyTotalPeople(rallyPostsDto.getRallyTotalPeople())
                .rallyRemainingPeople(rallyPostsDto.getRallyRemainingPeople())
                .rallyNotes(rallyPostsDto.getRallyNotes())
                .rallyStatus(RallyPostsStatusEnum.PUBLISHED.getCode())
                .rallyCreatedAt(LocalDateTime.now())
                .rallyUpdatedAt(LocalDateTime.now())
                .build();
        log.info("创建社交活动成功 - rallyPosts: {}", rallyPosts);
        rallyPostsMapper.insert(rallyPosts);
        int i = initRallyParticipant(rallyPosts, rallyApplicantId);
        if (i >= 0) {
            return rallyPosts;
        } else {
            return null;
        }
    }

    /**
     * 取消社交活动
     * @param rallyId 活动ID
     * @param userId 用户ID
     * @return 取消结果消息
     */
    @Override
    public String cancelRally(Long rallyId, Long userId) {
        RallyPosts rallyPosts = rallyPostsMapper.selectById(rallyId);
        if (rallyPosts == null) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POST_NOT_EXIST.getCode(), RallyResultEnum.RALLY_POST_NOT_EXIST.getMessage());
        }
        if (!userId.equals(rallyPosts.getInitiatorId())) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_CANCEL_NOT_AUTHORIZED.getCode(), RallyResultEnum.RALLY_POSTS_CANCEL_NOT_AUTHORIZED.getMessage());
        }
        rallyPosts.setRallyUpdatedAt(LocalDateTime.now());
        rallyPosts.setRallyStatus(RallyPostsStatusEnum.CANCELLED.getCode());
        int i = rallyPostsMapper.updateById(rallyPosts);
        if (i < 0) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_CANCEL_FAILURE.getCode(), RallyResultEnum.RALLY_POSTS_CANCEL_FAILURE.getMessage());
        }
        //todo 调用推送服务告诉用户社交活动取消成功
        return RallyResultEnum.RALLY_POSTS_CANCEL_SUCCESS.getMessage();
    }

    /**
     * 参加社交活动
     * @param postId 活动ID
     * @param userId 用户ID
     * @return 参加结果消息
     */
    @Override
    @Transactional
    public String joinRally(Long postId, Long userId) {
        //获取申请表
        RallyApplication rallyApplicationOlder = rallyApplicationMapper.selectByRallyIdAndApplicantId(postId, userId);

        //判断是否已经申请，或者已经取消申请
        if (rallyApplicationOlder != null && rallyApplicationOlder.getStatus() != RallyApplyStatusEnum.CANCELLED.getCode()) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_JOIN_HAS_APPLIED.getCode(), RallyResultEnum.RALLY_POSTS_JOIN_HAS_APPLIED.getMessage());
        } else if (rallyApplicationOlder != null) {
            rallyApplicationOlder.setAppliedAt(LocalDateTime.now());
            rallyApplicationOlder.setStatus(RallyApplyStatusEnum.PENDING.getCode());
            rallyApplicationMapper.updateById(rallyApplicationOlder);
            return RallyResultEnum.RALLY_POSTS_JOIN_SUCCESS.getMessage();
        }
        RallyPosts rallyPosts = rallyPostsMapper.selectByPostId(postId);
        if (rallyPosts == null) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POST_NOT_EXIST.getCode(), RallyResultEnum.RALLY_POST_NOT_EXIST.getMessage());
        }
        if (rallyPosts.getRallyEventDate().isBefore(LocalDate.now())) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_JOIN_HAS_EXPIRED.getCode(), RallyResultEnum.RALLY_POSTS_JOIN_HAS_EXPIRED.getMessage());
        }
        int rallyStatus = rallyPosts.getRallyStatus();
        if (rallyStatus == RallyPostsStatusEnum.FULL.getCode()) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_JOIN_HAS_FULL.getCode(), RallyResultEnum.RALLY_POSTS_JOIN_HAS_FULL.getMessage());
        }
        if (rallyStatus == RallyPostsStatusEnum.CANCELLED.getCode()) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_JOIN_HAS_CANCELLED.getCode(), RallyResultEnum.RALLY_POSTS_JOIN_HAS_CANCELLED.getMessage());
        }
        RallyApplication rallyApplication = RallyApplication.builder()
                .rallyPostId(postId)
                .applicantId(userId)
                .appliedAt(LocalDateTime.now())
                .status(RallyApplyStatusEnum.PENDING.getCode())
                .reviewedBy(rallyPosts.getInitiatorId())
                .build();
        rallyApplicationMapper.insert(rallyApplication);
        return RallyApplyStatusEnum.fromCode(rallyApplication.getStatus()).getDescription();
    }

    /**
     * 取消参加社交活动
     * @param rallyId 活动ID
     * @param userId 用户ID
     * @return 取消结果消息
     */
    @Override
    @Transactional
    public String cancelJoinRally(Long rallyId, Long userId) {
        RallyApplication rallyApplication = rallyApplicationMapper.selectByRallyIdAndApplicantId(rallyId, userId);
        if (rallyApplication == null) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_CANCEL_JOIN_NOT_EXIST.getCode(), RallyResultEnum.RALLY_POSTS_CANCEL_JOIN_NOT_EXIST.getMessage());
        }
        int status = rallyApplication.getStatus();
        if (status == RallyApplyStatusEnum.PENDING.getCode()) {
            rallyApplication.setStatus(RallyApplyStatusEnum.CANCELLED.getCode());
            log.info("rallyApplicationStatus--------------->: {}", rallyApplication.getStatus());
            rallyApplicationMapper.updateById(rallyApplication);
        } else if (status == RallyApplyStatusEnum.ACCEPTED.getCode()) {
            RallyPosts rallyPosts = rallyPostsMapper.selectByPostId(rallyId);
            rallyPosts.setRallyRemainingPeople(rallyPosts.getRallyRemainingPeople() + 1);
            rallyPosts.setRallyStatus(RallyPostsStatusEnum.PUBLISHED.getCode());
            RallyParticipant rallyParticipant = rallyParticipantMapper.getRallyApplicationByRallyPostIdAndParticipantId(rallyId, userId);
            rallyApplicationMapper.deleteById(rallyApplication);
            log.info("rallyParticipant: {}", rallyParticipant);
            rallyParticipantMapper.deleteById(rallyParticipant);
            log.info("rallyPosts: {}", rallyPosts);
            rallyPostsMapper.updateById(rallyPosts);
        }
        log.info("------------->{}", status);
        return RallyResultEnum.getMessageByCode(rallyApplication.getStatus());
    }

    /**
     * 审核社交活动申请
     * @param postId 活动ID
     * @param applicantId 申请人ID
     * @param inspectResult 审核结果
     * @param inspectorId 审核人ID
     * @return 审核结果消息
     */
    @Override
    @Transactional
    public String inspectRallyApply(Long postId, Long applicantId, int inspectResult, Long inspectorId) {
        RallyApplication rallyApplication = rallyApplicationMapper.selectByRallyIdAndApplicantId(postId, applicantId);
        // 判断申请是否存在
        if (rallyApplication == null){
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_INSPECT_NOT_EXIST.getCode(), RallyResultEnum.RALLY_POSTS_INSPECT_NOT_EXIST.getMessage());
        }

        log.info("rallyApplication: {}", rallyApplication);
        log.info("postId: {}", postId);
        log.info("applicantId: {}", applicantId);
        // 判断当前用户是否是球贴的发起者
        if (!inspectorId.equals(rallyApplication.getReviewedBy())) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_INSPECT_NOT_AUTHORIZED.getCode(), RallyResultEnum.RALLY_POSTS_INSPECT_NOT_AUTHORIZED.getMessage());
        }
        // 判断申请是否已经处理过
        if (rallyApplication.getStatus() == RallyApplyStatusEnum.PENDING.getCode()) {
            rallyApplication.setReviewedAt(LocalDateTime.now());
            rallyApplication.setStatus(inspectResult);
            rallyApplicationMapper.updateById(rallyApplication);
            // 处理通过申请
            if (inspectResult == RallyApplyStatusEnum.ACCEPTED.getCode()) {
                RallyParticipant rallyParticipant = RallyParticipant.builder()
                        .rallyPostId(postId)
                        .participantId(applicantId)
                        .joinedAt(LocalDateTime.now())
                        .isInitiator(IsInitiatorForRallyEnum.NO.getCode())
                        .build();
                log.info("----------------{}", rallyParticipant);
                rallyParticipantMapper.insert(rallyParticipant);
                RallyPosts rallyPosts = rallyPostsMapper.selectByPostId(postId);
                rallyPosts.setRallyRemainingPeople(rallyPosts.getRallyRemainingPeople() - 1);
                if (rallyPosts.getRallyRemainingPeople() == 0){
                    rallyPosts.setRallyStatus(RallyPostsStatusEnum.FULL.getCode());
                }
                rallyPostsMapper.updateById(rallyPosts);
                return RallyResultEnum.RALLY_POSTS_INSPECT_SUCCESS_PASS.getMessage();
            }
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_INSPECT_FAILURE.getCode(), RallyResultEnum.RALLY_POSTS_INSPECT_FAILURE.getMessage());
        }
        throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_INSPECT_HAS_CANCELLED.getCode(), RallyResultEnum.RALLY_POSTS_INSPECT_HAS_CANCELLED.getMessage());
    }

    /**
     * 更新社交活动信息
     * @param updateRallyDto 更新DTO
     * @param rallyId 活动ID
     * @param userid 用户ID
     * @return 更新结果消息
     */
    @Override
    public String updateRally(UpdateRallyDto updateRallyDto, Long rallyId, Long userid) {
        RallyPosts rallyPosts = rallyPostsMapper.selectByPostId(rallyId);
        if (rallyPosts == null) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POST_NOT_EXIST.getCode(), RallyResultEnum.RALLY_POST_NOT_EXIST.getMessage());
        }
        if (userid != rallyPosts.getInitiatorId()) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_UPDATE_NOT_AUTHORIZED.getCode(), RallyResultEnum.RALLY_POSTS_UPDATE_NOT_AUTHORIZED.getMessage());
        }
        List<RallyParticipant> rallyApplicationsList = rallyParticipantMapper.getRallyParticipantList(rallyId);
        rallyPosts.setRallyTitle(updateRallyDto.getRallyTitle());
        rallyPosts.setRallyRegion(updateRallyDto.getRallyRegion());
        rallyPosts.setRallyVenueName(updateRallyDto.getRallyVenueName());
        rallyPosts.setRallyCourtName(updateRallyDto.getRallyCourtName());
        rallyPosts.setRallyEventDate(updateRallyDto.getRallyEventDate());
        rallyPosts.setRallyTimeType(updateRallyDto.getRallyTimeType());
        rallyPosts.setRallyStartTime(updateRallyDto.getRallyStartTime());
        rallyPosts.setRallyEndTime(updateRallyDto.getRallyEndTime());
        rallyPosts.setRallyCost(updateRallyDto.getRallyCost());
        rallyPosts.setRallyCostBearer(updateRallyDto.getRallyCostBearer());
        rallyPosts.setRallyActivityType(updateRallyDto.getRallyActivityType());
        rallyPosts.setRallyGenderLimit(updateRallyDto.getRallyGenderLimit());
        rallyPosts.setRallyNtrpMin(updateRallyDto.getRallyNtrpMin());
        rallyPosts.setRallyNtrpMax(updateRallyDto.getRallyNtrpMax());
        rallyPosts.setRallyTotalPeople(updateRallyDto.getRallyTotalPeople());
        rallyPosts.setRallyRemainingPeople(updateRallyDto.getRallyTotalPeople()-rallyApplicationsList.size());
        rallyPosts.setRallyNotes(updateRallyDto.getRallyNotes());
        rallyPosts.setRallyUpdatedAt(LocalDateTime.now());
        rallyPostsMapper.updateById(rallyPosts);
        //Todo 推送服务推送给参与的人
        return RallyResultEnum.RALLY_POSTS_UPDATE_SUCCESS.getMessage();
    }

    /**
     * 获取我的活动列表
     * @param type 活动类型（1-我发起的，2-我参与的，3-我申请的，其他-全部）
     * @param page 页码
     * @param pageSize 每页大小
     * @param userId 用户ID
     * @return 分页结果
     */
    @Override
    public PaginationResult<RallyPostsVo> myActivities(Integer type, Integer page, Integer pageSize, Long userId) {
        int offset = (page - 1) * pageSize;

        // 根据类型进行不同查询
        switch (type) {
            case 0: // 全部 - 综合所有状态
                return getAllActivities(offset, pageSize, page, userId);
            case 1: // 已发布 - 用户自己创建的球局
                return getPublishedActivities(offset, pageSize, page, userId);
            case 2: // 已通过 - 用户申请且已通过的球局
                return getApprovedActivities(offset, pageSize, page, userId);
            case 3: // 申请中 - 用户申请待审核的球局
                return getPendingActivities(offset, pageSize, page, userId);
            case 4: // 已取消 - 用户创建或参与但已取消的球局
                return getCancelledActivities(offset, pageSize, page, userId);
            default:
                throw new GloboxApplicationException("无效的查询类型");
        }
    }

    /**
     * 获取审核列表
     * @param postId 活动ID
     * @param inspectorId 审核人ID
     * @return 申请列表
     */
    @Override
    public List<RallyApplicationVo> inspectList(Long postId,Long inspectorId) {
        List<RallyApplication> rallyApplicationByRallyId = rallyApplicationMapper.getRallyApplicationByRallyId(postId, inspectorId);
        List<RallyApplicationVo> list = rallyApplicationByRallyId.stream()
                .map(rallyApplication -> RallyApplicationVo.builder()
                        .id(rallyApplication.getApplicationId())
                        .rallyPostId(rallyApplication.getRallyPostId())
                        .applicantId(rallyApplication.getApplicantId())
                        .nickName(userDubboService.getUserInfo(rallyApplication.getApplicantId()).getNickName())
                        .avatarUrl(userDubboService.getUserInfo(rallyApplication.getApplicantId()).getAvatarUrl())
                        .userNtrpLevel(userDubboService.getUserInfo(rallyApplication.getApplicantId()).getUserNtrpLevel())
                        .inspectResult(RallyApplyStatusEnum.fromCode(rallyApplication.getStatus()).getDescription())
                        .appliedAt(rallyApplication.getAppliedAt())
                        .build()
                ).toList();

        return list;
    }

    @Override
    public RallyQueryVo getRallyQueryList() {
        List<FilterRegion> filterRegions = filterRegionMapper.selectAll();
        List<DictItem> areas = filterRegions.stream()
                .map(filterRegion -> DictItem.builder()
                        .value(Math.toIntExact(filterRegion.getFilterRegionId()))
                        .description(filterRegion.getFilterRegionName())
                        .build())
                .toList();
        List<DictItem> timeRange= Arrays.stream(TimeRangeType.values())
                .map(timeRangeType -> DictItem.builder()
                        .value(timeRangeType.getCode())
                        .description(timeRangeType.getDescription())
                        .build())
                .toList();
        List<DictItem> gender = Arrays.stream(RallyGenderLimitEnum.values())
                .map(genderType -> DictItem.builder()
                        .value(genderType.getCode())
                        .description(genderType.getMessage())
                        .build())
                .toList();
        List<DictItem> active = Arrays.stream(RallyActivityTypeEnum.values())
                .map(type -> DictItem.builder()
                        .value(type.getCode())
                        .description(type.getMessage())
                        .build())
                .toList();
        return RallyQueryVo.builder()
                .area(areas)
                .timeRange(timeRange)
                .genderLimit(gender)
                .activityType(active)
                .build();
    }

    /**
     * 将活动实体转换为视图对象
     * @param rallyPostsList 活动实体列表
     * @return 活动视图对象列表
     */
    private List<RallyPostsVo> rallyPostsToRallyPostsVo (List < RallyPosts > rallyPostsList) {

        List<RallyPostsVo> rallyPostsVoList = rallyPostsList.stream()
                .map(rallyPosts -> RallyPostsVo.builder()
                        .rallyPostId(rallyPosts.getRallyPostId())
                        .rallyInitiatorId(rallyPosts.getInitiatorId())
                        .avatarUrl(userDubboService.getUserInfo(rallyPosts.getInitiatorId()).getAvatarUrl())
                        .nickName(userDubboService.getUserInfo(rallyPosts.getInitiatorId()).getNickName())
                        .rallyTitle(rallyPosts.getRallyTitle())
                        .rallyRegion(rallyPosts.getRallyRegion())
                        .rallyVenueName(rallyPosts.getRallyVenueName())
                        .rallyCourtName(rallyPosts.getRallyCourtName())
                        .rallyEventDate(rallyPosts.getRallyEventDate())
                        .rallyStartTime(rallyPosts.getRallyStartTime())
                        .rallyEndTime(rallyPosts.getRallyEndTime())
                        .ntrpMin(rallyPosts.getRallyNtrpMin())
                        .rallyLabel(getRallyLabel(rallyPosts))
                        .ntrpMax(rallyPosts.getRallyNtrpMax())
                        .rallyTotalPeople(rallyPosts.getRallyTotalPeople())
                        .rallyStatus(RallyPostsStatusEnum.fromCode(rallyPosts.getRallyStatus()).getDescription())
                        .rallyParticipants(rallyParticipantVos(rallyPosts.getRallyPostId()))
                        .createdAt(rallyPosts.getRallyCreatedAt())
                        .build()).toList();
        return rallyPostsVoList;
    }

    /**
     * 初始化活动参与者
     * @param rallyPosts 活动实体
     * @param rallyApplicantId 申请人ID
     * @return 插入结果
     */
    private int initRallyParticipant (RallyPosts rallyPosts, Long rallyApplicantId){
        RallyParticipant rallyParticipant = RallyParticipant.builder()
                .rallyPostId(rallyPosts.getRallyPostId())
                .participantId(rallyApplicantId)
//                .userNtrp(userDubboService.getUserInfo(rallyApplicantId).getNtrp())
                .joinedAt(LocalDateTime.now())
                .isInitiator(IsInitiatorForRallyEnum.YES.getCode())
                .build();
        log.info("创建社交活动成功 - rallyParticipant: {}", rallyParticipant);
        int insert = rallyParticipantMapper.insert(rallyParticipant);
        return insert;
    }

    /**
     * 获取活动参与者视图对象列表
     * @param postId 活动ID
     * @return 参与者视图对象列表
     */
    private List<RallyParticipantVo> rallyParticipantVos(Long postId){
        List<RallyParticipant> rallyApplicationsList = rallyParticipantMapper.getRallyParticipantList(postId);
        log.info("rallyApplicationsList: {}", rallyApplicationsList);
        List<RallyParticipantVo> rallyParticipantsVoList = rallyApplicationsList.stream()
                .map(participant -> RallyParticipantVo.builder()
                        .participantId(participant.getParticipantId())
                        .avatarUrl(userDubboService.getUserInfo(participant.getParticipantId()).getAvatarUrl())
                        .nickName(userDubboService.getUserInfo(participant.getParticipantId()).getNickName())
                        .userNtrp(participant.getUserNtrp())
                        .joinedAt(participant.getJoinedAt())
                        .isInitiator(participant.getIsInitiator()==1)
                        .build())
                .toList();
        return rallyParticipantsVoList;
    }

    /**
     * 获取活动标签
     * @param rallyPosts 活动实体
     * @return 标签列表
     */
    private List<String> getRallyLabel(RallyPosts rallyPosts){
        List<String> labels = new ArrayList<>();
        labels.add(RallyActivityTypeEnum.getByCode(rallyPosts.getRallyActivityType()).getMessage());
        labels.add(RallyGenderLimitEnum.getByCode(rallyPosts.getRallyGenderLimit()).getMessage());
        labels.add(rallyPosts.getRallyCourtName());
        return labels;
    }

    /**
     * 获取全部活动（综合所有状态）
     */
    private PaginationResult<RallyPostsVo> getAllActivities(int offset, int pageSize, int page, Long userId) {
        // 分别查询各种状态的数据
        List<RallyPostsVo> allActivities = new ArrayList<>();

        // 1. 获取已发布的活动
        List<RallyPosts> publishedList = rallyPostsMapper.myActivities(0, Integer.MAX_VALUE, userId);
        allActivities.addAll(rallyPostsToRallyPostsVo(publishedList));

        // 2. 获取已通过的活动
        List<RallyParticipant> approvedParticipants = rallyParticipantMapper.getRallyApplicationByParticipantIdAndStatus(
                userId,0, 0, Integer.MAX_VALUE);
        List<Long> approvedIds = approvedParticipants.stream()
                .map(RallyParticipant::getRallyPostId)
                .distinct()
                .collect(Collectors.toList());
        if (!approvedIds.isEmpty()) {
            List<RallyPosts> approvedPosts = rallyPostsMapper.selectBatchIds(approvedIds);
            allActivities.addAll(rallyPostsToRallyPostsVo(approvedPosts));
        }

        // 3. 获取申请中的活动
        List<RallyApplication> pendingApplications = rallyApplicationMapper.getRallyApplicationByApplicantIdAndStatus(
                userId, RallyApplyStatusEnum.PENDING.getCode(), 0, Integer.MAX_VALUE);
        List<Long> pendingIds = pendingApplications.stream()
                .map(RallyApplication::getRallyPostId)
                .distinct()
                .collect(Collectors.toList());
        if (!pendingIds.isEmpty()) {
            List<RallyPosts> pendingPosts = rallyPostsMapper.selectBatchIds(pendingIds);
            allActivities.addAll(rallyPostsToRallyPostsVo(pendingPosts));
        }

        // 4. 获取已取消的活动（包括自己取消的和参与的活动被取消的）
        List<RallyPosts> cancelledPosts = rallyPostsMapper.getCancelledActivities(userId, 0, Integer.MAX_VALUE);
        allActivities.addAll(rallyPostsToRallyPostsVo(cancelledPosts));

        // 去重并排序（按时间倒序）
        List<RallyPostsVo> distinctActivities = allActivities.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                RallyPostsVo::getRallyPostId,
                                item -> item,
                                (existing, replacement) -> existing,
                                LinkedHashMap::new
                        ),
                        map -> new ArrayList<>(map.values())
                ));

        // 按创建时间倒序排序
        distinctActivities.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        // 分页处理
        int total = distinctActivities.size();
        int startIndex = Math.min(offset, total);
        int endIndex = Math.min(offset + pageSize, total);

        List<RallyPostsVo> pagedList = startIndex < total ?
                distinctActivities.subList(startIndex, endIndex) :
                new ArrayList<>();

        return PaginationResult.build(pagedList, total, page, pageSize);
    }

    /**
     * 获取已发布的活动（用户自己创建的球局）
     */
    private PaginationResult<RallyPostsVo> getPublishedActivities(int offset, int pageSize, int page, Long userId) {
        List<RallyPosts> rallyPostsList = rallyPostsMapper.getPublishedActivitiesByUser(
                userId, offset, pageSize);
        List<RallyPostsVo> rallyPostsVos = rallyPostsToRallyPostsVo(rallyPostsList);
        int total = rallyPostsMapper.countPublishedActivitiesByUser(userId);

        return PaginationResult.build(rallyPostsVos, total, page, pageSize);
    }

    /**
     * 获取已通过的活动（用户申请且已通过）
     */
    private PaginationResult<RallyPostsVo> getApprovedActivities(int offset, int pageSize, int page, Long userId) {
        // 查询用户已通过的申请
        int total = rallyParticipantMapper.countByParticipantIdAndStatus(
                userId,0);

        List<RallyParticipant> approvedParticipants = rallyParticipantMapper.getRallyApplicationByParticipantIdAndStatus(
                userId,0, offset, pageSize);

        if (approvedParticipants.isEmpty()) {
            return PaginationResult.build(new ArrayList<>(), 0, page, pageSize);
        }

        List<Long> rallyPostIds = approvedParticipants.stream()
                .map(RallyParticipant::getRallyPostId)
                .distinct()
                .collect(Collectors.toList());

        List<RallyPosts> rallyPostsList = rallyPostsMapper.selectBatchIds(rallyPostIds);
        List<RallyPostsVo> rallyPostsVos = rallyPostsToRallyPostsVo(rallyPostsList);

        return PaginationResult.build(rallyPostsVos, total, page, pageSize);
    }

    /**
     * 获取申请中的活动
     */
    private PaginationResult<RallyPostsVo> getPendingActivities(int offset, int pageSize, int page, Long userId) {
        int total = rallyApplicationMapper.countByApplicantIdAndStatus(
                userId, RallyApplyStatusEnum.PENDING.getCode());

        List<RallyApplication> pendingApplications = rallyApplicationMapper.getRallyApplicationByApplicantIdAndStatus(
                userId, RallyApplyStatusEnum.PENDING.getCode(), offset, pageSize);

        if (pendingApplications.isEmpty()) {
            return PaginationResult.build(new ArrayList<>(), 0, page, pageSize);
        }

        List<Long> rallyPostIds = pendingApplications.stream()
                .map(RallyApplication::getRallyPostId)
                .distinct()
                .collect(Collectors.toList());

        List<RallyPosts> rallyPostsList = rallyPostsMapper.selectBatchIds(rallyPostIds);
        List<RallyPostsVo> rallyPostsVos = rallyPostsToRallyPostsVo(rallyPostsList);

        return PaginationResult.build(rallyPostsVos, total, page, pageSize);
    }

    /**
     * 获取已取消的活动
     */
    private PaginationResult<RallyPostsVo> getCancelledActivities(int offset, int pageSize, int page, Long userId) {
        // 查询已取消的活动（包括用户创建的和参与的）
        List<RallyPosts> cancelledList = rallyPostsMapper.getCancelledActivities(userId, offset, pageSize);
        List<RallyPostsVo> rallyPostsVos = rallyPostsToRallyPostsVo(cancelledList);
        int total = rallyPostsMapper.countCancelledActivities(userId);
        return PaginationResult.build(rallyPostsVos, total, page, pageSize);
    }


}

