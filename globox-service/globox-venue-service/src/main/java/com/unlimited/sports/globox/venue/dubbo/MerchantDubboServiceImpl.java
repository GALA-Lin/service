package com.unlimited.sports.globox.venue.dubbo;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.MerchantErrorCode;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.VenueCode;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.dubbo.merchant.MerchantDubboService;
import com.unlimited.sports.globox.dubbo.merchant.dto.*;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.model.venue.vo.VenueSnapshotVo;
import com.unlimited.sports.globox.venue.dto.ActivityPreviewContext;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.venue.dto.SlotBookingContext;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.venue.constants.BookingCacheConstants;

import com.unlimited.sports.globox.common.lock.RedisDistributedLock;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;

import com.unlimited.sports.globox.model.venue.entity.venues.ActivityType;
import com.unlimited.sports.globox.venue.service.*;
import com.unlimited.sports.globox.venue.constants.BookingConstants;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivityParticipant;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商家RPC服务实现  - 为订单服务提供价格查询功能
 */
@Component
@DubboService(group = "rpc")
@Slf4j
public class MerchantDubboServiceImpl implements MerchantDubboService {

    @Autowired
    private CourtMapper courtMapper;

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Autowired
    private IVenueActivityParticipantService participantService;

    @Value("${merchant.mooncourt.id}")
    private Long mooncourtId;

    @Autowired
    private IBookingService bookingService;

    @DubboReference(group = "rpc")
    private UserDubboService userDubboService;


    /**
     * 真正占据槽位
     * @param dto 包含用户ID、预定日期以及预定槽位列表的请求对象
     */
    @Override
    public RpcResult<PricingResultDto> quoteVenue(PricingRequestDto dto) {
        if (dto == null || dto.getSlotIds() == null || dto.getSlotIds().isEmpty()) {
            log.warn("请求参数不合法");
            return RpcResult.error(VenueCode.BOOKING_SLOT_INFO_EMPTY);
        }

        try {
            // 验证并准备预订上下文- 第一次验证（获取锁前）
            SlotBookingContext context =
                    bookingService.validateAndPrepareBookingContext(dto.getSlotIds(), dto.getBookingDate(), dto.getUserId());
            List<String> lockKeys = dto.getSlotIds().stream()
                    .map(slotId -> buildLockKey(slotId, dto.getBookingDate()))
                    .collect(Collectors.toList());

            List<RLock> locks = null;

            try {
                // 获取用户信息（昵称）
                String userName = BookingConstants.DEFAULT_USER_NAME;
                RpcResult<UserInfoVo> userInfoResult = userDubboService.getUserInfo(dto.getUserId());
                Assert.rpcResultOk(userInfoResult);
                UserInfoVo userInfo = userInfoResult.getData();
                if (userInfo != null && userInfo.getNickName() != null) {
                    userName = userInfo.getNickName();
                }

                // 批量获取所有槽位的锁
                locks = redisDistributedLock.tryLockMultiple(lockKeys, 1, -1L, TimeUnit.SECONDS);
                if (locks == null) {
                    log.warn("获取分布式锁失败 - userId: {}, slotTemplateIds: {}", dto.getUserId(), dto.getSlotIds());
                    return RpcResult.error(VenueCode.SLOT_BEING_BOOKED);
                }
                log.info("【锁包事务】成功获取分布式锁 - userId: {}, 槽位数: {}", dto.getUserId(), dto.getSlotIds().size());

                PricingResultDto result = bookingService.executeBookingInTransaction(
                        dto,
                        context,
                        userName);

                return RpcResult.ok(result);

            } finally {
                // 释放锁（无论事务成功还是失败）
                if (locks != null) {
                    redisDistributedLock.unlockMultiple(locks);
                    log.info("【锁包事务】释放分布式锁 - userId: {}", dto.getUserId());
                }
            }
        } catch (GloboxApplicationException e) {
            log.error("预订槽位失败 - userId: {}, code: {}, error: {}", dto.getUserId(), e.getCode(), e.getMessage(), e);
            return RpcResult.error(VenueCode.fromCode(e.getCode()));
        } catch (Exception e) {
            log.error("预订槽位发生未知错误 - userId: {}", dto.getUserId(), e);
            return RpcResult.error(VenueCode.VENUE_BOOKING_FAIL);
        }
    }

    @Override
    public RpcResult<PricingActivityResultDto> quoteVenueActivity(PricingActivityRequestDto dto) {
        // 参数验证
        if (dto == null || dto.getActivityId() == null || dto.getUserId() == null ||
            dto.getQuantity() == null || dto.getQuantity() <= 0) {
            log.warn("活动报名请求参数不合法");
            return RpcResult.error(VenueCode.ACTIVITY_PARAM_INVALID);
        }

        try {
            // 验证并准备活动上下文
            ActivityPreviewContext context = bookingService.validateAndPrepareActivityContext(dto.getActivityId());
            VenueActivity activity = context.getActivity();
            Venue venue = context.getVenue();
            Court court = context.getCourt();
            ActivityType activityType = context.getActivityType();

            // 校验活动类型
            if (activityType == null) {
                log.warn("不支持的活动类型 - activityId: {}, activityTypeId: {}",
                        dto.getActivityId(), activity.getActivityTypeId());
                throw new GloboxApplicationException(VenueCode.ACTIVITY_TYPE_NOT_SUPPORTED);
            }

            // 校验用户报名数量是否超过限制（activity.maxQuotaPerUser为null表示不限制）
            Integer maxQuota = activity.getMaxQuotaPerUser();
            if (maxQuota != null && dto.getQuantity() > maxQuota) {
                log.warn("活动报名数量超过限制 - activityId: {}, userId: {}, 请求数量: {}, 最大允许: {}",
                        dto.getActivityId(), dto.getUserId(), dto.getQuantity(), maxQuota);
                throw new GloboxApplicationException(VenueCode.ACTIVITY_QUOTA_EXCEEDED);
            }

            // 执行批量用户报名（原子操作，全部成功或全部失败）
            List<VenueActivityParticipant> participants = participantService.registerMultipleParticipants(
                    dto.getActivityId(),
                    dto.getUserId(),
                    dto.getQuantity(),
                    dto.getUserPhone());

            log.info("用户活动批量报名成功 - activityId: {}, userId: {}, quantity: {}, phone: {}",
                    dto.getActivityId(), dto.getUserId(), participants.size(), dto.getUserPhone());

            // 为每个参与记录构建一个 RecordQuote（recordId 使用 participantId）
            List<RecordQuote> activityQuotes = participants.stream()
                    .map(participant -> RecordQuote.builder()
                            .recordId(participant.getParticipantId())
                            .courtId(activity.getCourtId())
                            .courtName(court.getName())
                            .bookingDate(activity.getActivityDate())
                            .startTime(activity.getStartTime())
                            .endTime(activity.getEndTime())
                            .unitPrice(activity.getUnitPrice() != null ? activity.getUnitPrice() : BigDecimal.ZERO)
                            .recordExtras(Collections.emptyList())
                            .build())
                    .collect(Collectors.toList());

            // 构建返回结果
            PricingActivityResultDto result = PricingActivityResultDto.builder()
                    .recordQuote(activityQuotes)
                    .orderLevelExtras(Collections.emptyList())
                    .sourcePlatform(venue.getVenueType())
                    .sellerName(venue.getName())
                    .sellerId(activity.getVenueId())
                    .bookingDate(activity.getActivityDate())
                    .activityId(activity.getActivityId())
                    .activityTypeCode(activityType.getTypeCode())
                    .activityTypeName(activity.getActivityTypeDesc())
                    .build();

            return RpcResult.ok(result);
        } catch (GloboxApplicationException e) {
            log.error("活动报名失败 - activityId: {}, userId: {}, quantity: {}, code: {}, error: {}",
                    dto.getActivityId(), dto.getUserId(), dto.getQuantity(), e.getCode(), e.getMessage(), e);
            return RpcResult.error(VenueCode.fromCode(e.getCode()));
        } catch (Exception e) {
            log.error("活动报名发生未知错误 - activityId: {}, userId: {}, quantity: {}",
                    dto.getActivityId(), dto.getUserId(), dto.getQuantity(), e);
            return RpcResult.error(VenueCode.VENUE_BOOKING_FAIL);
        }
    }

    @Override
    public RpcResult<VenueSnapshotResultDto> getVenueSnapshot(VenueSnapshotRequestDto dto) {
        try {
            // 查询场馆信息
            Venue venue = venueMapper.selectById(dto.getVenueId());
            if (venue == null) {
                log.warn("场馆不存在: venueId={}", dto.getVenueId());
                return RpcResult.error(VenueCode.VENUE_NOT_EXIST);
            }

            VenueSnapshotVo venueSnapshotVo = bookingService.getVenueSnapshotVo(dto.getLatitude(),dto.getLongitude() ,venue);
            // 查询场地信息
            List<Court> courts = courtMapper.selectBatchIds(dto.getCourtId());
            if (courts.isEmpty()) {
                return RpcResult.error(VenueCode.COURT_NOT_EXIST);
            }

            // 构建场地快照列表
            List<VenueSnapshotResultDto.CourtSnapshotDto> courtSnapshots = courts.stream()
                    .map(court -> VenueSnapshotResultDto.CourtSnapshotDto.builder()
                            .id(court.getCourtId())
                            .name(court.getName())
                            .groundType(court.getGroundType())
                            .courtType(court.getCourtType())
                            .build())
                    .collect(Collectors.toList());

            // 将 Vo 转换为 Dto（添加场地快照列表）
            VenueSnapshotResultDto result = VenueSnapshotResultDto.builder()
                    .id(venueSnapshotVo.getId())
                    .name(venueSnapshotVo.getName())
                    .phone(venueSnapshotVo.getPhone())
                    .region(venueSnapshotVo.getRegion())
                    .address(venueSnapshotVo.getAddress())
                    .coverImage(venueSnapshotVo.getCoverImage())
                    .distance(venueSnapshotVo.getDistance())
                    .facilities(venueSnapshotVo.getFacilities())
                    .courtSnapshotDtos(courtSnapshots)
                    .build();

            return RpcResult.ok(result);
        } catch (GloboxApplicationException e) {
            log.error("获取场馆快照失败 - venueId: {}, code: {}, error: {}",
                    dto.getVenueId(), e.getCode(), e.getMessage(), e);
            return RpcResult.error(VenueCode.fromCode(e.getCode()));
        } catch (Exception e) {
            log.error("获取场馆快照发生未知错误 - venueId: {}", dto.getVenueId(), e);
            return RpcResult.error(VenueCode.VENUE_BOOKING_FAIL);
        }
    }


    /**
     * 获取揽月的场馆 id 列表
     * @return
     */
    @Override
    public RpcResult<List<Long>> getMoonCourtIdList() {
        List<Venue> venues = venueMapper.selectVenuesByMerchantId(mooncourtId);
        if (ObjectUtils.isEmpty(venues)) {
            return RpcResult.error(MerchantErrorCode.MERCHANT_VENUE_NOT_EXIST);
        }
        List<Long> resultList = venues.stream().map(Venue::getVenueId).toList();
        return RpcResult.ok(resultList);
    }


    /**
     * 构建锁键
     */
    private String buildLockKey(Long slotTemplateId, LocalDate bookingDate) {
        return BookingCacheConstants.BOOKING_LOCK_KEY_PREFIX + slotTemplateId + BookingCacheConstants.BOOKING_LOCK_KEY_SEPARATOR + bookingDate;
    }

}
