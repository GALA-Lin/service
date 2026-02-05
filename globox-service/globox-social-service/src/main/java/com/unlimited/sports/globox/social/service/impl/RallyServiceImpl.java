package com.unlimited.sports.globox.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.constants.RallyMQConstants;
import com.unlimited.sports.globox.common.enums.RegionCityEnum;
import com.unlimited.sports.globox.common.enums.social.RallyTimeTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.message.social.RallyStartingReminderMessage;
import com.unlimited.sports.globox.common.result.SocialCode;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.dubbo.governance.SensitiveWordsDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoRequest;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoResponse;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.dubbo.user.RegionDubboService;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.RegionDto;
import com.unlimited.sports.globox.model.social.dto.RallyPostsDto;
import com.unlimited.sports.globox.model.social.dto.RallyQueryDto;
import com.unlimited.sports.globox.model.social.dto.UpdateRallyDto;
import com.unlimited.sports.globox.model.social.entity.*;
import com.unlimited.sports.globox.model.social.vo.*;
import com.unlimited.sports.globox.social.mapper.RallyApplicationMapper;
import com.unlimited.sports.globox.social.mapper.RallyParticipantMapper;
import com.unlimited.sports.globox.social.mapper.RallyPostsMapper;
import com.unlimited.sports.globox.social.service.RallyService;
import com.unlimited.sports.globox.social.util.SocialNotificationUtil;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ObjectUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.unlimited.sports.globox.model.social.entity.RallyPostsStatusEnum.fromCode;


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

    @DubboReference(group = "rpc")
    private RegionDubboService regionDubboService;

    @DubboReference(group = "rpc")
    private UserDubboService userDubboService;

    @DubboReference(group = "rpc")
    private SensitiveWordsDubboService sensitiveWordsDubboService;

    @Autowired
    private SocialNotificationUtil socialNotificationUtil;

    @Autowired
    private MQService mqService;

    /**
     * 约球开始前多久提醒（单位：秒）
     */
    @Value("${rally.reminder.advance.seconds:3600}")
    private int reminderAdvanceSeconds;

    /**
     * 获取社交活动列表
     * @param rallyQueryDto 查询条件
     * @return 分页结果
     */
    @Override
    public RallyQueryVo getRallyPostsList(RallyQueryDto rallyQueryDto) {
        int offset = (rallyQueryDto.getPage() - 1) * rallyQueryDto.getPageSize();
        log.info("获取社交活动列表 - rallyQueryDto:{}", rallyQueryDto);
        String area = rallyQueryDto.getArea();
        log.info("筛选地址 - area:{}", area);
        List<String> areaList = null;
        if (area != null && !area.isEmpty()){
            areaList = List.of(area.split(","));
        }
        log.info("筛选地址 - areaList:{}", areaList);
        List<RallyPosts> rallyPostsList = rallyPostsMapper.getRallyPostsList(areaList, rallyQueryDto.getTimeRange(), rallyQueryDto.getGenderLimit(),rallyQueryDto.getNtrpMin(),rallyQueryDto.getNtrpMax(),rallyQueryDto.getActivityType(), offset, rallyQueryDto.getPageSize());

        List<RallyPostsVo> rallyPostsVos = rallyPostsToRallyPostsVo(rallyPostsList);
        Long count = rallyPostsMapper.countRallyPostsList(areaList, rallyQueryDto.getTimeRange(), rallyQueryDto.getGenderLimit(),rallyQueryDto.getNtrpMin(),rallyQueryDto.getNtrpMax(),rallyQueryDto.getActivityType());
        PaginationResult<RallyPostsVo> rallyList = PaginationResult.build(
                rallyPostsVos,
                count,
                rallyQueryDto.getPage(),
                rallyQueryDto.getPageSize());
        RallyQueryVo rallyQueryList = getRallyQueryList();
        rallyQueryList.setRallyPostsVoList(rallyList);

        return rallyQueryList;
    }

    /**
     * 获取社交活动详情
     * @param postId 活动ID
     * @param rallyApplicantId 申请人ID
     * @return 活动详情
     */
    @Override
    public RallyPostsDetailsVo getRallyDetails(Long postId, Long rallyApplicantId) {
        RallyPosts rallyPosts = rallyPostsMapper.selectOne(
                Wrappers.<RallyPosts>lambdaQuery()
                        .eq(RallyPosts::getRallyPostId, postId)
        );
        boolean isOwner = false;
        if (rallyPosts == null) {
            return null;
        }

        if (rallyPosts.getInitiatorId().equals(rallyApplicantId)){
            isOwner = true;
        }
        int rallyApplicationStatus;
        RallyApplication rallyApplication = rallyApplicationMapper.selectOne(
                Wrappers.<RallyApplication>lambdaQuery()
                        .eq(RallyApplication::getRallyPostId, rallyPosts.getRallyPostId())
                        .eq(!ObjectUtils.isEmpty(rallyApplicantId), RallyApplication::getApplicantId, rallyApplicantId)
        );
        if (rallyApplication == null) {
            rallyApplicationStatus = RallyApplyStatusEnum.DEFAULT.getCode();
        } else {
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
                .rallyParticipantCount(rallyParticipantVos.size())
                .rallyEventDate(rallyPosts.getRallyEventDate())
                .rallyStartTime(rallyPosts.getRallyStartTime())
                .rallyEndTime(rallyPosts.getRallyEndTime())
                .rallyTimeType(rallyPosts.getRallyTimeType().getDescription())
                .rallyGenderLimit(rallyPosts.getRallyGenderLimit())
                .ntrpMin(rallyPosts.getRallyNtrpMin())
                .ntrpMax(rallyPosts.getRallyNtrpMax())
                .rallyTotalPeople(rallyPosts.getRallyTotalPeople())
                .rallyRemainingPeople(rallyPosts.getRallyRemainingPeople())
                .currentPeopleCount(rallyPosts.getRallyTotalPeople() - rallyPosts.getRallyRemainingPeople())
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
    @Transactional(rollbackFor = Exception.class)
    public RallyPosts createRally(RallyPostsDto rallyPostsDto, Long rallyApplicantId) {

        log.info("创建社交活动 - rallyPostsDto: {}, rallyApplicantId: {}", rallyPostsDto, rallyApplicantId);

        // 校验rallyNtrpMin小于等于rallyNtrpMax
        if (rallyPostsDto.getRallyNtrpMin() > rallyPostsDto.getRallyNtrpMax()) {
            throw new GloboxApplicationException(SocialCode.RALLY_NTRP_MIN_GT_MAX);
        }
        // 校验日期是否在今天之前（已有的逻辑）
        if (rallyPostsDto.getRallyEventDate().isBefore(LocalDate.now())){
            throw new GloboxApplicationException(SocialCode.RALLY_EVENT_DATE_BEFORE_NOW);
        }

        if (rallyPostsDto.getRallyEventDate().isEqual(LocalDate.now())) {
            // 只有在填写了开始时间的情况下才进行校验
            if (rallyPostsDto.getRallyStartTime() != null && rallyPostsDto.getRallyStartTime().isBefore(LocalTime.now())) {
                throw new GloboxApplicationException(SocialCode.RALLY_EVENT_DATE_BEFORE_NOW);
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append(rallyPostsDto.getRallyTitle())
                .append('\n');
        if (!ObjectUtils.isEmpty(rallyPostsDto.getRallyVenueName())) {
            builder.append(rallyPostsDto.getRallyVenueName())
                    .append('\n');
        }
        if (!ObjectUtils.isEmpty(rallyPostsDto.getRallyCourtName())) {
            builder.append(rallyPostsDto.getRallyCourtName())
                    .append('\n');
        }

        RpcResult<Void> voidRpcResult = sensitiveWordsDubboService.checkSensitiveWords(builder.toString());
        Assert.rpcResultOk(voidRpcResult);
        RallyPosts rallyPosts = RallyPosts.builder()
                .initiatorId(rallyApplicantId)
                .rallyTitle(rallyPostsDto.getRallyTitle())
                .rallyRegion(rallyPostsDto.getRallyRegion())
                .rallyVenueName(rallyPostsDto.getRallyVenueName())
                .rallyCourtName(rallyPostsDto.getRallyCourtName())
                .rallyEventDate(rallyPostsDto.getRallyEventDate())
                .rallyTimeType(RallyTimeTypeEnum.fromCode(rallyPostsDto.getRallyTimeType()))
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
        if (i <= 0) {
            throw new GloboxApplicationException("初始化参与者失败");
        }
        // 如果指定了时间，发送延迟消息到队列（提前1小时提醒）
        if (rallyPosts.getRallyEventDate() != null && rallyPosts.getRallyStartTime() != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendRallyStartingReminderDelayedMessage(rallyPosts);
                }
            });
        }
        return rallyPosts;
    }

    /**
     * 取消社交活动
     * @param rallyId 活动ID
     * @param userId 用户ID
     * @return 取消结果消息
     */
    @Override
    @Transactional
    public String cancelRally(Long rallyId, Long userId) {
        log.info("取消约球{}",rallyId);
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

        // 发送约球取消通知给所有参与者
        sendRallyCancelledNotifications(rallyId, rallyPosts);

        return RallyResultEnum.RALLY_POSTS_CANCEL_SUCCESS.getMessage();
    }

    /**
     * 发送约球取消通知给所有参与者（排除发起人）
     */
    private void sendRallyCancelledNotifications(Long rallyId, RallyPosts rallyPosts) {
        try {
            // 查询所有参与者（排除发起人）
            List<RallyParticipant> participants = rallyParticipantMapper.selectList(
                    Wrappers.<RallyParticipant>lambdaQuery()
                            .eq(RallyParticipant::getRallyPostId, rallyId)
                            .ne(RallyParticipant::getParticipantId, rallyPosts.getInitiatorId())
            );

            if (participants.isEmpty()) {
                log.info("[约球取消] 没有参与者需要通知 - rallyId={}", rallyId);
                return;
            }

            // 为每个参与者发送取消通知
            for (RallyParticipant participant : participants) {
                try {
                    socialNotificationUtil.sendRallyCancelledNotification(rallyId, participant.getParticipantId(), rallyPosts);
                } catch (Exception e) {
                    log.error("[约球取消] 发送参与者通知失败 - rallyId={}, participantId={}, error={}",
                            rallyId, participant.getParticipantId(), e.getMessage());
                    // 单个参与者通知失败不影响其他参与者通知继续发送
                }
            }

            log.info("[约球取消] 参与者通知发送完成 - rallyId={}, 参与者数量={}", rallyId, participants.size());
        } catch (Exception e) {
            log.error("[约球取消] 发送参与者通知失败 - rallyId={}", rallyId, e);
        }
    }
    private boolean isRallyExpired(RallyPosts rally) {
        if (rally.getRallyEventDate() == null || rally.getRallyStartTime() == null) {
            return false;
        }
        // 合并日期和开始时间
        LocalDateTime startDateTime = LocalDateTime.of(rally.getRallyEventDate(), rally.getRallyStartTime());
        // 如果当前时间已经过了开始时间，则视为过期/已完成
        return startDateTime.isBefore(LocalDateTime.now());
    }

    /**
     * 申请参加社交活动
     * @param postId 活动ID
     * @param userId 用户ID
     * @return 参加结果消息
     */
    @Override
    @Transactional
    public String joinRally(Long postId, Long userId) {
        //获取申请表
        RallyApplication rallyApplicationOlder = rallyApplicationMapper.selectOne(
                Wrappers.<RallyApplication>lambdaQuery()
                        .eq(RallyApplication::getRallyPostId, postId)
                        .eq(RallyApplication::getApplicantId, userId)
        );
        if(isRallyExpired(rallyPostsMapper.selectById(postId))){
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_JOIN_HAS_EXPIRED.getCode(), RallyResultEnum.RALLY_POSTS_JOIN_HAS_EXPIRED.getMessage());
        }
        //判断是否已经申请且未取消
        if (rallyApplicationOlder != null && rallyApplicationOlder.getStatus() != RallyApplyStatusEnum.CANCELLED.getCode()) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_JOIN_HAS_APPLIED.getCode(), RallyResultEnum.RALLY_POSTS_JOIN_HAS_APPLIED.getMessage());
        } else if (rallyApplicationOlder != null) {
            rallyApplicationOlder.setAppliedAt(LocalDateTime.now());
            rallyApplicationOlder.setStatus(RallyApplyStatusEnum.PENDING.getCode());
            rallyApplicationMapper.updateById(rallyApplicationOlder);
            return RallyResultEnum.RALLY_POSTS_JOIN_SUCCESS.getMessage();
        }

        RallyPosts rallyPosts = rallyPostsMapper.selectOne(
                Wrappers.<RallyPosts>lambdaQuery()
                        .eq(RallyPosts::getRallyPostId, postId)
        );

        if (rallyPosts == null) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POST_NOT_EXIST.getCode(), RallyResultEnum.RALLY_POST_NOT_EXIST.getMessage());
        }
        // 禁止发起人报名自己的活动
        if (rallyPosts.getInitiatorId().equals(userId)) {
            throw new GloboxApplicationException(
                    RallyResultEnum.RALLY_POSTS_APPLY_SELF_FORBIDDEN.getCode(),
                    "您不能报名自己发起的活动"
            );
        }


        RpcResult<UserInfoVo> applicantInfoRpcResult = userDubboService.getUserInfo(userId);
        Assert.rpcResultOk(applicantInfoRpcResult);
        UserInfoVo applicantInfo = applicantInfoRpcResult.getData();

        // 判断性别是否要求
        int genderLimit = rallyPosts.getRallyGenderLimit(); // 性别限制: 0=不限 1=仅男生 2=仅女生
        Integer userGender = applicantInfo.getGender().getCode();    // 1=男, 0=女
        log.info("约球性别要求：{} -- 申请人性别：{}", genderLimit == 1 ? "仅男生":"仅女生" , applicantInfo.getGender());
        if (genderLimit != 0) { // 如果有限制
            if (genderLimit == 1 && (userGender == null || userGender != 1)) {
                throw new GloboxApplicationException("该活动仅限男生参加");
            }
            if (genderLimit == 2 && (userGender == null || userGender != 0)) {
                throw new GloboxApplicationException("该活动仅限女生参加");
            }
        }
        // 判断Ntrp是否符合要求
        double userNtrp = applicantInfo.getUserNtrpLevel();
        if (userNtrp <= rallyPosts.getRallyNtrpMin() || userNtrp >= rallyPosts.getRallyNtrpMax()) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_JOIN_NTRP_LIMIT.getMessage());
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

        // 发送约球参与申请通知给发起人
        socialNotificationUtil.sendRallyApplicationNotification(postId, userId, rallyPosts);

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

        // 检查球局当前状态
        RallyPosts currentrallyPosts = rallyPostsMapper.selectById(rallyId);
        if (currentrallyPosts == null) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POST_NOT_EXIST.getCode(),
                    RallyResultEnum.RALLY_POST_NOT_EXIST.getMessage());
        }

        RallyApplication rallyApplication = rallyApplicationMapper.selectOne(
                Wrappers.<RallyApplication>lambdaQuery()
                        .eq(RallyApplication::getRallyPostId, rallyId)
                        .eq(RallyApplication::getApplicantId, userId)
        );
        if (rallyApplication == null) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_CANCEL_JOIN_NOT_EXIST);
        }
        int status = rallyApplication.getStatus();
        if (rallyApplication.getStatus() == RallyApplyStatusEnum.CANCELLED.getCode()) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_CANCEL_JOIN_ALREADY_CANCELLED);
        }
        if (status == RallyApplyStatusEnum.PENDING.getCode()) {
            rallyApplication.setStatus(RallyApplyStatusEnum.CANCELLED.getCode());
            log.info("约球申请状态: {}", rallyApplication.getStatus());
            rallyApplicationMapper.updateById(rallyApplication);

        } else if (status == RallyApplyStatusEnum.ACCEPTED.getCode()) {
            // 原子操作修复：直接通过 SQL 增加人数并处理状态回滚
            int updated = rallyPostsMapper.incrementRemainingPeople(rallyId);
            if (updated <= 0) {
                throw new GloboxApplicationException("取消失败，活动状态异常");
            }

            // 删除参与记录和申请记录
            rallyParticipantMapper.delete(Wrappers.<RallyParticipant>lambdaQuery()
                    .eq(RallyParticipant::getRallyPostId, rallyId)
                    .eq(RallyParticipant::getParticipantId, userId));
            rallyApplicationMapper.deleteById(rallyApplication.getApplicationId());

            // 发送异步通知
            socialNotificationUtil.sendRallyQuitNotification(rallyId, userId, currentrallyPosts);
        }
        log.info("约球状态变更为{}", status);
        return RallyResultEnum.RALLY_POSTS_CANCEL_SUCCESS.getMessage();
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
    @Transactional(rollbackFor = Exception.class)
    public String inspectRallyApply(Long postId, Long applicantId, int inspectResult, Long inspectorId) {
        RallyApplication rallyApplication = rallyApplicationMapper.selectOne(
                Wrappers.<RallyApplication>lambdaQuery()
                        .eq(RallyApplication::getRallyPostId, postId)
                        .eq(RallyApplication::getApplicantId, applicantId)
        );
        // 判断申请是否存在
        if (rallyApplication == null){
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_INSPECT_NOT_EXIST.getCode(), RallyResultEnum.RALLY_POSTS_INSPECT_NOT_EXIST.getMessage());
        }
        // 判断当前用户是否是球贴的发起者
        if (!inspectorId.equals(rallyApplication.getReviewedBy())) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_INSPECT_NOT_AUTHORIZED.getCode(), RallyResultEnum.RALLY_POSTS_INSPECT_NOT_AUTHORIZED.getMessage());
        }
        // 判断申请是否已经处理过
        if (rallyApplication.getStatus() == RallyApplyStatusEnum.PENDING.getCode()) {
            // 处理通过申请
            if (inspectResult == RallyApplyStatusEnum.ACCEPTED.getCode()) {
                RallyParticipant existingParticipant = rallyParticipantMapper.selectOne(
                        Wrappers.<RallyParticipant>lambdaQuery()
                                .eq(RallyParticipant::getRallyPostId, postId)
                                .eq(RallyParticipant::getParticipantId, applicantId)
                );
                if (existingParticipant != null) {
                    rallyApplication.setReviewedAt(LocalDateTime.now());
                    rallyApplication.setStatus(RallyApplyStatusEnum.ACCEPTED.getCode());
                    rallyApplicationMapper.updateById(rallyApplication);
                    return RallyResultEnum.RALLY_POSTS_INSPECT_SUCCESS_PASS.getMessage();
                }
                // 1. 原子扣减人数 (防止超卖)
                int updatedRows = rallyPostsMapper.decrementRemainingPeopleIfAvailable(postId);
                if (updatedRows == 0) {
                    // 如果更新失败，说明此刻可能刚好满员了，或者帖子状态变了
                    throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_JOIN_HAS_FULL.getCode(), RallyResultEnum.RALLY_POSTS_JOIN_HAS_FULL.getMessage());
                }
                RallyParticipant rallyParticipant = RallyParticipant.builder()
                        .rallyPostId(postId)
                        .participantId(applicantId)
                        .joinedAt(LocalDateTime.now())
                        .isInitiator(IsInitiatorForRallyEnum.NO.getCode())
                        .build();
                rallyParticipantMapper.insert(rallyParticipant);
                RallyPosts rallyPosts = rallyPostsMapper.selectById(postId);

                // 根据人数是否已满，发送不同的通知给申请人
                if (rallyPosts.getRallyStatus() == RallyPostsStatusEnum.FULL.getCode()) {
                    this.autoRejectOtherApplications(postId, rallyPosts, inspectorId);
                    socialNotificationUtil.sendRallyFullNotification(postId, rallyPosts, inspectorId);
                } else {
                    // 人数未满，发送普通的申请被接受通知
                    socialNotificationUtil.sendRallyApplicationAcceptedNotification(postId, applicantId, rallyPosts);
                }

                rallyApplication.setReviewedAt(LocalDateTime.now());
                rallyApplication.setStatus(RallyApplyStatusEnum.ACCEPTED.getCode());
                rallyApplicationMapper.updateById(rallyApplication);
                return RallyResultEnum.RALLY_POSTS_INSPECT_SUCCESS_PASS.getMessage();
            } else if (inspectResult == RallyApplyStatusEnum.REJECTED.getCode()) {

                RallyPosts rallyPosts = rallyPostsMapper.selectById(postId);

                // 检查是否因为人数已满而拒绝
                if (rallyPosts != null && rallyPosts.getRallyStatus() == RallyPostsStatusEnum.FULL.getCode()) {
                    // 人数已满，发送人数已满拒绝通知
                    socialNotificationUtil.sendRallyParticipantsFullRejectedNotification(postId, applicantId, rallyPosts);
                }
                rallyApplication.setReviewedAt(LocalDateTime.now());
                rallyApplication.setStatus(RallyApplyStatusEnum.REJECTED.getCode());
                rallyApplicationMapper.updateById(rallyApplication);
                return RallyApplyStatusEnum.REJECTED.getDescription();
            }
            rallyApplication.setReviewedAt(LocalDateTime.now());
            rallyApplication.setStatus(inspectResult);
            rallyApplicationMapper.updateById(rallyApplication);
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

        // 查询原始数据
        RallyPosts rallyPosts = rallyPostsMapper.selectById(rallyId);
        if (rallyPosts == null) {
            throw new GloboxApplicationException(
                    RallyResultEnum.RALLY_POST_NOT_EXIST.getCode(),
                    RallyResultEnum.RALLY_POST_NOT_EXIST.getMessage()
            );
        }

        // 权限校验
        if (!Objects.equals(userid, rallyPosts.getInitiatorId())) {
            throw new GloboxApplicationException(
                    RallyResultEnum.RALLY_POSTS_UPDATE_NOT_AUTHORIZED.getCode(),
                    RallyResultEnum.RALLY_POSTS_UPDATE_NOT_AUTHORIZED.getMessage()
            );
        }

        // 检查总人数是否减少到小于当前参与人数
        Long currentParticipantCount = rallyParticipantMapper.selectCount(
                Wrappers.<RallyParticipant>lambdaQuery()
                        .eq(RallyParticipant::getRallyPostId, rallyId)
        );

        if (updateRallyDto.getRallyTotalPeople() < currentParticipantCount) {
            throw new GloboxApplicationException(
                    "总人数不能小于当前参与人数(" + currentParticipantCount + ")"
            );
        }


        rallyPosts.setRallyTitle(updateRallyDto.getRallyTitle());
        rallyPosts.setRallyRegion(updateRallyDto.getRallyRegion());
        rallyPosts.setRallyVenueName(updateRallyDto.getRallyVenueName());
        rallyPosts.setRallyCourtName(updateRallyDto.getRallyCourtName());
        rallyPosts.setRallyEventDate(updateRallyDto.getRallyEventDate());
        rallyPosts.setRallyTimeType(RallyTimeTypeEnum.fromCode(updateRallyDto.getRallyTimeType()));
        rallyPosts.setRallyStartTime(updateRallyDto.getRallyStartTime());
        rallyPosts.setRallyEndTime(updateRallyDto.getRallyEndTime());
        rallyPosts.setRallyCost(updateRallyDto.getRallyCost());
        rallyPosts.setRallyCostBearer(updateRallyDto.getRallyCostBearer());
        rallyPosts.setRallyActivityType(updateRallyDto.getRallyActivityType());
        rallyPosts.setRallyGenderLimit(updateRallyDto.getRallyGenderLimit());
        rallyPosts.setRallyNtrpMin(updateRallyDto.getRallyNtrpMin());
        rallyPosts.setRallyNtrpMax(updateRallyDto.getRallyNtrpMax());
        rallyPosts.setRallyNotes(updateRallyDto.getRallyNotes());
        rallyPosts.setRallyUpdatedAt(LocalDateTime.now());
        rallyPostsMapper.updateById(rallyPosts);
        // 使用数据库计算更新人数
        int updated = rallyPostsMapper.updateRemainingPeopleByCalculation(
                rallyId,
                updateRallyDto.getRallyTotalPeople()
        );
        log.info("更新人数操作完成数: {}", updated);
        // 更新状态
        RallyPosts updatedPosts = rallyPostsMapper.selectById(rallyId);
        if (updatedPosts.getRallyRemainingPeople() == 0
                && updatedPosts.getRallyStatus() != RallyPostsStatusEnum.FULL.getCode()) {
            // 人数已满，更新状态
            updatedPosts.setRallyStatus(RallyPostsStatusEnum.FULL.getCode());
            rallyPostsMapper.updateById(updatedPosts);
        } else if (updatedPosts.getRallyRemainingPeople() > 0
                && updatedPosts.getRallyStatus() == RallyPostsStatusEnum.FULL.getCode()) {
            // 有空位，恢复为已发布
            updatedPosts.setRallyStatus(RallyPostsStatusEnum.PUBLISHED.getCode());
            rallyPostsMapper.updateById(updatedPosts);
        }
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
        return switch (type) {
            case 0 -> // 全部 - 综合所有状态
                    getAllActivities(offset, pageSize, page, userId);
            case 1 -> // 已通过 - 用户申请且已通过的球局
                    getApprovedActivities(offset, pageSize, page, userId);
            case 2 -> // 申请中 - 用户申请待审核的球局
                    getPendingActivities(offset, pageSize, page, userId);
            case 3 -> // 已发布 - 用户自己创建的球局
                    getPublishedActivities(offset, pageSize, page, userId);
            case 4 -> // 已取消 - 用户创建或参与但已取消的球局
                    getCancelledActivities(offset, pageSize, page, userId);
            default -> throw new GloboxApplicationException("无效的查询类型");
        };
    }

    /**
     * 获取审核列表
     * @param postId 活动ID
     * @param inspectorId 审核人ID
     * @return 申请列表
     */
    @Override
    public PaginationResult<RallyApplicationVo> inspectList(Long postId, Integer page, Integer pageSize, Long inspectorId) {

        // 1. 存在性与权属校验
        RallyPosts posts = rallyPostsMapper.selectById(postId);
        if (posts == null) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POST_NOT_EXIST);
        }
        if (!posts.getInitiatorId().equals(inspectorId)) {
            throw new GloboxApplicationException(RallyResultEnum.RALLY_POSTS_INSPECT_NOT_AUTHORIZED.getCode(), "无权查看此球局的申请列表");
        }

        // 2. 分页参数健壮性处理（避免null、非法值导致异常）
        int currentPage = Optional.ofNullable(page).filter(p -> p >= 1).orElse(1);
        int currentPageSize = Optional.ofNullable(pageSize).filter(ps -> ps >= 1 && ps <= 100).orElse(10); // 限制最大页大小，防止一次性查询过多数据

        // 3. 抽取公共查询条件，避免冗余，提升可维护性
        LambdaQueryWrapper<RallyApplication> rallyApplicationLambdaQueryWrapper = Wrappers.<RallyApplication>lambdaQuery()
                .eq(RallyApplication::getRallyPostId, postId)
                .eq(RallyApplication::getReviewedBy, inspectorId)
                .ne(RallyApplication::getStatus, RallyApplyStatusEnum.ACCEPTED.getCode())
                .orderByDesc(RallyApplication::getAppliedAt);

        // 4. 使用MyBatis-Plus Page对象实现优雅分页（无SQL拼接，安全高效）
        Page<RallyApplication> rallyApplicationPage = new Page<>(currentPage, currentPageSize);
        Page<RallyApplication> resultPage = rallyApplicationMapper.selectPage(rallyApplicationPage, rallyApplicationLambdaQueryWrapper);

        // 5. 解决N+1问题：批量查询用户信息，提升性能，避免循环调用Dubbo接口
        List<RallyApplication> rallyApplications = resultPage.getRecords();
        if (CollectionUtils.isEmpty(rallyApplications)) {
            // 无数据时直接返回空分页结果，避免后续无效操作
            return PaginationResult.build(Collections.emptyList(), 0L, currentPage, currentPageSize);
        }
        // 提取所有申请人ID
        List<Long> applicantIds = rallyApplications.stream()
                .map(RallyApplication::getApplicantId)
                .distinct() // 去重，减少Dubbo接口调用压力
                .toList();

        BatchUserInfoRequest batchUserInfoRequest = new BatchUserInfoRequest();
        batchUserInfoRequest.setUserIds(applicantIds);

        // 5.3 调用实际Dubbo批量查询接口
        RpcResult<BatchUserInfoResponse> batchUserInfoRpcResult = userDubboService.batchGetUserInfo(batchUserInfoRequest);
        Assert.rpcResultOk(batchUserInfoRpcResult);

        // 5.4 处理返回结果，转换为Map<用户ID, 用户信息>，方便后续快速取值（避免循环遍历列表）
        BatchUserInfoResponse batchUserInfoResponse = batchUserInfoRpcResult.getData();
        Map<Long, UserInfoVo> userInfoMap = new HashMap<>();
        if (batchUserInfoResponse != null && !CollectionUtils.isEmpty(batchUserInfoResponse.getUsers())) {
            userInfoMap = batchUserInfoResponse.getUsers().stream()
                    .collect(Collectors.toMap(
                            UserInfoVo::getUserId,
                            userInfoVo -> userInfoVo,
                            (existingValue, newValue) -> existingValue
                    ));
        }

        // 6. 转换为VO列表（从批量查询的Map中获取用户信息，避免循环调用）
        Map<Long, UserInfoVo> finalUserInfoMap = userInfoMap;
        List<RallyApplicationVo> UserInfoList = rallyApplications.stream()
                .map(rallyApplication -> {
                    // 兜底处理：若未查询到用户信息，创建空UserInfoVo避免空指针
                    UserInfoVo userInfo = finalUserInfoMap.getOrDefault(rallyApplication.getApplicantId(), new UserInfoVo());
                    return RallyApplicationVo.builder()
                            .id(rallyApplication.getApplicationId())
                            .rallyPostId(rallyApplication.getRallyPostId())
                            .applicantId(rallyApplication.getApplicantId())
                            .nickName(userInfo.getNickName())
                            .avatarUrl(userInfo.getAvatarUrl())
                            .userNtrpLevel(userInfo.getUserNtrpLevel())
                            .inspectResult(RallyApplyStatusEnum.fromCode(rallyApplication.getStatus()).getDescription())
                            .appliedAt(rallyApplication.getAppliedAt())
                            .build();
                })
                .toList();

        // 7. 构建分页结果（直接从MyBatis-Plus Page对象中获取总条数，无需手动查询）
        return PaginationResult.build(
                UserInfoList,
                resultPage.getTotal(),
                currentPage,
                currentPageSize
        );
    }


    private RallyQueryVo getRallyQueryList() {
        // TODO 目前只查询成都市
        RpcResult<List<RegionDto>> listRpcResult = regionDubboService.listDistrictsByCity(RegionCityEnum.CHENG_DU);
        Assert.rpcResultOk(listRpcResult);
        List<RegionDto> regionList = listRpcResult.getData();
        List<String> regionsList = regionList.stream().map(RegionDto::getName).toList();

        List<String> timeRangeList = new ArrayList<>();
        for (TimeRangeType value : TimeRangeType.values()) {
            timeRangeList.add(value.getDescription());
        }
        List<String> genderList = new ArrayList<>();
        for (RallyGenderLimitEnum value : RallyGenderLimitEnum.values()) {
            genderList.add(value.getMessage());
        }
        List<String> activityList = new ArrayList<>();
        for (RallyActivityTypeEnum value : RallyActivityTypeEnum.values()) {
            activityList.add(value.getMessage());
        }
        return RallyQueryVo.builder()
                .area(regionsList)
                .timeRange(timeRangeList)
                .genderLimit(genderList)
                .activityType(activityList)
                .build();
    }

    /**
     * 将活动实体转换为视图对象
     * @param rallyPostsList 活动实体列表
     * @return 活动视图对象列表
     */
    private List<RallyPostsVo> rallyPostsToRallyPostsVo (List<RallyPosts> rallyPostsList) {

        // 批量获取用户信息，避免N+1查询
        List<Long> userIds = rallyPostsList.stream()
                .map(RallyPosts::getInitiatorId)
                .collect(Collectors.toList());
        BatchUserInfoRequest batchUserInfoRequest = new BatchUserInfoRequest();

        batchUserInfoRequest.setUserIds(userIds);

        RpcResult<BatchUserInfoResponse> batchResult = userDubboService.batchGetUserInfo(batchUserInfoRequest);
        Assert.rpcResultOk(batchResult);
        List<UserInfoVo> userInfoResult =  batchResult.getData().getUsers();
        Map<Long, UserInfoVo> userInfoMap = userInfoResult.stream()
                .collect(Collectors.toMap(UserInfoVo::getUserId, Function.identity()));

        return rallyPostsList.stream()
                .map(rallyPosts -> {
                    UserInfoVo userInfo = userInfoMap.get(rallyPosts.getInitiatorId());
                    return RallyPostsVo.builder()
                            .rallyPostId(rallyPosts.getRallyPostId())
                            .rallyInitiatorId(rallyPosts.getInitiatorId())
                            .avatarUrl(userInfo.getAvatarUrl())
                            .nickName(userInfo.getNickName())
                            .rallyTitle(rallyPosts.getRallyTitle())
                            .rallyRegion(rallyPosts.getRallyRegion())
                            .rallyVenueName(rallyPosts.getRallyVenueName())
                            .rallyCourtName(rallyPosts.getRallyCourtName())
                            .rallyEventDate(rallyPosts.getRallyEventDate())
                            .rallyStartTime(rallyPosts.getRallyStartTime())
                            .rallyEndTime(rallyPosts.getRallyEndTime())
                            .rallyTimeType(rallyPosts.getRallyTimeType().getDescription())
                            .ntrpMin(rallyPosts.getRallyNtrpMin())
                            .rallyLabel(getRallyLabel(rallyPosts))
                            .ntrpMax(rallyPosts.getRallyNtrpMax())
                            .rallyTotalPeople(rallyPosts.getRallyTotalPeople())
                            .rallyStatusCode(rallyPosts.getRallyStatus())
                            .rallyStatus(fromCode(rallyPosts.getRallyStatus()).getDescription())
                            .rallyParticipants(rallyParticipantVos(rallyPosts.getRallyPostId()))
                            .createdAt(rallyPosts.getRallyCreatedAt())
                            .currentPeopleCount(rallyPosts.getRallyTotalPeople() - rallyPosts.getRallyRemainingPeople())
                            .rallyRemainingPeople(rallyPosts.getRallyRemainingPeople())
                            .build();
                }).toList();
    }

    /**
     * 初始化活动参与者
     * @param rallyPosts 活动实体
     * @param rallyApplicantId 申请人ID
     * @return 插入结果
     */
    private int initRallyParticipant (RallyPosts rallyPosts, Long rallyApplicantId){
        RpcResult<UserInfoVo> rpcResult = userDubboService.getUserInfo(rallyApplicantId);
        Assert.rpcResultOk(rpcResult);
        UserInfoVo userInfo = rpcResult.getData();
        RallyParticipant rallyParticipant = RallyParticipant.builder()
                .rallyPostId(rallyPosts.getRallyPostId())
                .participantId(rallyApplicantId)
                .userNtrp(userInfo.getUserNtrpLevel()!=null?
                        userInfo.getUserNtrpLevel():0 )
                .joinedAt(LocalDateTime.now())
                .isInitiator(IsInitiatorForRallyEnum.YES.getCode())
                .build();
        log.info("创建社交活动成功 - rallyParticipant: {}", rallyParticipant);
        return rallyParticipantMapper.insert(rallyParticipant);
    }

    /**
     * 获取活动参与者视图对象列表
     * @param postId 活动ID
     * @return 参与者视图对象列表
     */
    private List<RallyParticipantVo> rallyParticipantVos(Long postId){

        List<RallyParticipant> rallyParticipantList = rallyParticipantMapper.selectList(
                Wrappers.<RallyParticipant>lambdaQuery()
                        .eq(RallyParticipant::getRallyPostId, postId)
        );
        return rallyParticipantList.stream()
                .map(participant -> {
                    RpcResult<UserInfoVo> rpcResult = userDubboService.getUserInfo(participant.getParticipantId());
                    Assert.rpcResultOk(rpcResult);
                    UserInfoVo userInfo = rpcResult.getData();
                    return RallyParticipantVo.builder()
                            .participantId(participant.getParticipantId())
                            .avatarUrl(userInfo.getAvatarUrl())
                            .nickName(userInfo.getNickName())
                            .userNtrp(userInfo.getUserNtrpLevel())
                            .joinedAt(participant.getJoinedAt())
                            .isInitiator(participant.getIsInitiator()==1)
                            .build();
                })
                .toList();
    }

    /**
     * 获取活动标签
     * @param rallyPosts 活动实体
     * @return 标签列表
     */
    private List<String> getRallyLabel(RallyPosts rallyPosts){
        List<String> labels = new ArrayList<>();
        labels.add(Objects.requireNonNull(RallyActivityTypeEnum.getByCode(rallyPosts.getRallyActivityType())).getMessage());
        labels.add(RallyGenderLimitEnum.getByCode(rallyPosts.getRallyGenderLimit()).getMessage());
        labels.add("NTRP:"+ rallyPosts.getRallyNtrpMin());
        return labels;
    }
    /**
     * 获取全部活动（综合所有状态）
     */
    private PaginationResult<RallyPostsVo> getAllActivities(int offset, int pageSize, int page, Long userId) {
        // 分别查询各种状态的数据

        // 1. 获取已发布的活动
        List<RallyPosts> publishedList = rallyPostsMapper.selectList(
                Wrappers.<RallyPosts>lambdaQuery()
                        .eq(RallyPosts::getInitiatorId, userId)
                        .orderByDesc(RallyPosts::getRallyCreatedAt)
                        .last("LIMIT " + 0 + ", " + Integer.MAX_VALUE)
        );
        List<RallyPostsVo> allActivities = new ArrayList<>(rallyPostsToRallyPostsVo(publishedList));

        // 2. 获取已通过的活动
        List<RallyParticipant> approvedParticipants = rallyParticipantMapper.selectList(
                Wrappers.<RallyParticipant>lambdaQuery()
                        .eq(RallyParticipant::getParticipantId, userId)
                        .eq(RallyParticipant::getIsVoluntarilyCancel, 0)
                        .last("LIMIT " + 0 + ", " + Integer.MAX_VALUE)
        );
        List<Long> approvedIds = approvedParticipants.stream()
                .map(RallyParticipant::getRallyPostId)
                .distinct()
                .collect(Collectors.toList());
        if (!approvedIds.isEmpty()) {
            List<RallyPosts> approvedPosts = rallyPostsMapper.selectBatchIds(approvedIds);
            allActivities.addAll(rallyPostsToRallyPostsVo(approvedPosts));
        }

        // 3. 获取申请中的活动
        List<RallyApplication> pendingApplications = rallyApplicationMapper.selectList(
                Wrappers.<RallyApplication>lambdaQuery()
                        .eq(RallyApplication::getApplicantId, userId)
                        .eq(RallyApplication::getStatus, RallyApplyStatusEnum.PENDING.getCode())
                        .last("LIMIT " + 0 + ", " + Integer.MAX_VALUE)
        );
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
        List<RallyPosts> rallyPostsList = rallyPostsMapper.selectList(
                Wrappers.<RallyPosts>lambdaQuery()
                        .eq(RallyPosts::getInitiatorId, userId)
                        .eq(RallyPosts::getRallyStatus, RallyPostsStatusEnum.PUBLISHED.getCode())
                        .last("LIMIT " + offset + ", " + pageSize)
        );
        List<RallyPostsVo> rallyPostsVos = rallyPostsToRallyPostsVo(rallyPostsList);
        Long total = rallyPostsMapper.selectCount(
                Wrappers.<RallyPosts>lambdaQuery()
                        .eq(RallyPosts::getInitiatorId, userId)
                        .eq(RallyPosts::getRallyStatus, RallyPostsStatusEnum.PUBLISHED.getCode())
        );

        return PaginationResult.build(rallyPostsVos, total, page, pageSize);
    }

    /**
     * 获取已通过的活动（用户申请且已通过）
     */
    private PaginationResult<RallyPostsVo> getApprovedActivities(int offset, int pageSize, int page, Long userId) {
        // 查询用户已通过的申请

        Long total = rallyParticipantMapper.selectCount(
                Wrappers.<RallyParticipant>lambdaQuery()
                        .eq(RallyParticipant::getParticipantId, userId)
                        .eq(RallyParticipant::getIsVoluntarilyCancel, 0)
        );
        List<RallyParticipant> approvedParticipants = rallyParticipantMapper.selectList(
                Wrappers.<RallyParticipant>lambdaQuery()
                        .eq(RallyParticipant::getParticipantId, userId)
                        .eq(RallyParticipant::getIsVoluntarilyCancel, 0)
                        .last("LIMIT " + offset + ", " + pageSize)
        );

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
        Long total = rallyApplicationMapper.selectCount(
                Wrappers.<RallyApplication>lambdaQuery()
                        .eq(RallyApplication::getApplicantId, userId)
                        .eq(RallyApplication::getStatus, RallyApplyStatusEnum.PENDING.getCode())
        );

        List<RallyApplication> pendingApplications = rallyApplicationMapper.selectList(
                Wrappers.<RallyApplication>lambdaQuery()
                        .eq(RallyApplication::getApplicantId, userId)
                        .eq(RallyApplication::getStatus, RallyApplyStatusEnum.PENDING.getCode())
                        .last("LIMIT " + offset + ", " + pageSize)
        );
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

    /**
     * 发送约球即将开始的延迟提醒消息
     * 延迟时间 = 约球时间 - 1小时
     * @param rallyPosts 约球信息
     */
    private void sendRallyStartingReminderDelayedMessage(RallyPosts rallyPosts) {
        try {
            RallyStartingReminderMessage message = RallyStartingReminderMessage.builder()
                    .rallyId(rallyPosts.getRallyPostId())
                    .build();

            // 计算约球开始时间和当前时间，得出延迟时间（毫秒）
            // 约球时间 - 提前指定秒数为提醒时间
            LocalDateTime rallyStartDateTime = LocalDateTime.of(
                    rallyPosts.getRallyEventDate(),
                    rallyPosts.getRallyStartTime()
            );
            LocalDateTime reminderDateTime = rallyStartDateTime.minusSeconds(reminderAdvanceSeconds);
            LocalDateTime nowDateTime = LocalDateTime.now();

            // 如果提醒时间已过，不发送
            if (reminderDateTime.isBefore(nowDateTime)) {
                log.info("约球提醒时间已过，不发送延迟消息 - rallyId={}, reminderTime={}",
                        rallyPosts.getRallyPostId(), reminderDateTime);
                return;
            }

            long delayMillis = Duration.between(nowDateTime, reminderDateTime).toMillis();
            // 检查延迟时间是否合理
            if (delayMillis > Integer.MAX_VALUE) {
                log.warn("延迟时间超过上限，不发送延迟消息 - rallyId={}, delayMillis={}",
                        rallyPosts.getRallyPostId(), delayMillis);
                return;
            }

            if (delayMillis < 0) {
                log.warn("延迟时间为负数，不发送延迟消息 - rallyId={}, delayMillis={}",
                        rallyPosts.getRallyPostId(), delayMillis);
                return;
            }

            mqService.sendDelay(
                    RallyMQConstants.EXCHANGE_TOPIC_RALLY_STARTING_REMINDER,
                    RallyMQConstants.ROUTING_RALLY_STARTING_REMINDER,
                    message,
                    (int)delayMillis); //Integer.MAX_VALUE毫秒（约24天），会溢出
            log.info("约球提醒延迟消息已发送 - rallyId={}, delay={}ms, reminderTime={}",
                    rallyPosts.getRallyPostId(), delayMillis, reminderDateTime);
        } catch (Exception e) {
            log.error("发送约球提醒延迟消息失败 - rallyId={}, error={}",
                    rallyPosts.getRallyPostId(), e.getMessage(), e);
            // 延迟消息发送失败不影响约球创建
        }
    }

    /**
     * 自动拒绝多余申请并发送通知
     */
    private void autoRejectOtherApplications(Long postId, RallyPosts rallyPosts, Long inspectorId) {
        // 1. 查询所有还在排队的申请人
        List<RallyApplication> pendingApps = rallyApplicationMapper.selectList(
                Wrappers.<RallyApplication>lambdaQuery()
                        .eq(RallyApplication::getRallyPostId, postId)
                        .eq(RallyApplication::getStatus, RallyApplyStatusEnum.PENDING.getCode())
        );

        if (ObjectUtils.isEmpty(pendingApps)) return;

        // 2. 批量更新数据库状态
        rallyApplicationMapper.update(null, Wrappers.<RallyApplication>lambdaUpdate()
                .set(RallyApplication::getStatus, RallyApplyStatusEnum.REJECTED.getCode())
                .set(RallyApplication::getReviewedAt, LocalDateTime.now())
                .set(RallyApplication::getReviewedBy, inspectorId)
                .eq(RallyApplication::getRallyPostId, postId)
                .eq(RallyApplication::getStatus, RallyApplyStatusEnum.PENDING.getCode())
        );

        // 3. 循环发送“人数已满”通知
        for (RallyApplication app : pendingApps) {
            try {
                socialNotificationUtil.sendRallyParticipantsFullRejectedNotification(postId, app.getApplicantId(), rallyPosts);
            } catch (Exception e) {
                log.error("发送满员拒绝通知失败, userId: {}", app.getApplicantId(), e);
            }
        }
    }

}
