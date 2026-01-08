package com.unlimited.sports.globox.venue.dubbo;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.VenueCode;
import com.unlimited.sports.globox.dubbo.merchant.MerchantDubboService;
import com.unlimited.sports.globox.dubbo.merchant.dto.*;
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
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivityParticipant;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

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


    @Autowired
    private IBookingService bookingService;


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

            // 提取上下文数据
            List<VenueBookingSlotTemplate> templates = context.getTemplates();
            Venue venue = context.getVenue();
            Map<Long, String> courtNameMap = context.getCourtNameMap();

            List<String> lockKeys = dto.getSlotIds().stream()
                    .map(slotId -> buildLockKey(slotId, dto.getBookingDate()))
                    .collect(Collectors.toList());

            List<RLock> locks = null;

            try {
                // 批量获取所有槽位的锁
                locks = redisDistributedLock.tryLockMultiple(lockKeys, 1, -1L, TimeUnit.SECONDS);
                if (locks == null) {
                    log.warn("获取分布式锁失败 - userId: {}, slotTemplateIds: {}", dto.getUserId(), dto.getSlotIds());
                    return RpcResult.error(VenueCode.SLOT_BEING_BOOKED);
                }
                log.info("【锁包事务】成功获取分布式锁 - userId: {}, 槽位数: {}", dto.getUserId(), dto.getSlotIds().size());

                PricingResultDto result = bookingService.executeBookingInTransaction(
                        dto,
                        templates,
                        venue,
                        courtNameMap);

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
        if (dto == null || dto.getActivityId() == null || dto.getUserId() == null) {
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

            // 执行用户报名（原子操作，防止超卖）
            try {
                VenueActivityParticipant participant = participantService.registerUserToActivity(
                        dto.getActivityId(),
                        dto.getUserId());

                log.info("用户活动报名成功 - activityId: {}, userId: {}, participantId: {}",
                        dto.getActivityId(), dto.getUserId(), participant.getParticipantId());
            } catch (GloboxApplicationException e) {
                log.error("用户报名活动失败:{}",e.getMessage(),e);
                return RpcResult.error(VenueCode.fromCode(e.getCode()));
            }

            // 构建活动价格信息
            RecordQuote activityQuote = RecordQuote.builder()
                    .recordId(activity.getActivityId())
                    .courtId(activity.getCourtId())
                    .courtName(court.getName())
                    .bookingDate(activity.getActivityDate())
                    .startTime(activity.getStartTime())
                    .endTime(activity.getEndTime())
                    .unitPrice(activity.getUnitPrice() != null ? activity.getUnitPrice() : BigDecimal.ZERO)
                    .recordExtras(Collections.emptyList())
                    .build();

            // 构建返回结果
            PricingActivityResultDto result = PricingActivityResultDto.builder()
                    .recordQuote(Collections.singletonList(activityQuote))
                    .orderLevelExtras(Collections.emptyList())
                    .sourcePlatform(venue.getVenueType())
                    .sellerName(venue.getName())
                    .sellerId(activity.getVenueId())
                    .bookingDate(activity.getActivityDate())
                    .activityId(activity.getActivityId())
                    .activityTypeCode(activityType != null ? activityType.getTypeCode() : null)
                    .activityTypeName(activity.getActivityTypeDesc())
                    .build();

            return RpcResult.ok(result);
        } catch (GloboxApplicationException e) {
            log.error("活动报名失败 - activityId: {}, userId: {}, code: {}, error: {}",
                    dto.getActivityId(), dto.getUserId(), e.getCode(), e.getMessage(), e);
            return RpcResult.error(VenueCode.fromCode(e.getCode()));
        } catch (Exception e) {
            log.error("活动报名发生未知错误 - activityId: {}, userId: {}",
                    dto.getActivityId(), dto.getUserId(), e);
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
     * 构建锁键
     */
    private String buildLockKey(Long slotTemplateId, LocalDate bookingDate) {
        return BookingCacheConstants.BOOKING_LOCK_KEY_PREFIX + slotTemplateId + BookingCacheConstants.BOOKING_LOCK_KEY_SEPARATOR + bookingDate;
    }

}
