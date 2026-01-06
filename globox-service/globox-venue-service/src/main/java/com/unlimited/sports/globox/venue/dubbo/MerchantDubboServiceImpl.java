package com.unlimited.sports.globox.venue.dubbo;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.dubbo.merchant.MerchantDubboService;
import com.unlimited.sports.globox.dubbo.merchant.dto.*;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotRecord;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.venue.enums.BookingSlotStatus;
import com.unlimited.sports.globox.model.venue.enums.OperatorSourceEnum;
import com.unlimited.sports.globox.venue.constants.BookingCacheConstants;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotRecordMapper;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotTemplateMapper;
import com.unlimited.sports.globox.venue.lock.RedisDistributedLock;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplateMapper;
import com.unlimited.sports.globox.venue.mapper.VenueActivityMapper;
import com.unlimited.sports.globox.venue.mapper.ActivityTypeMapper;
import com.unlimited.sports.globox.model.venue.entity.venues.ActivityType;
import com.unlimited.sports.globox.venue.service.*;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivityParticipant;
import com.unlimited.sports.globox.common.utils.DistanceUtils;
import com.unlimited.sports.globox.merchant.mapper.VenueFacilityRelationMapper;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueFacilityRelation;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.mybatis.spring.SqlSessionTemplate;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 商家RPC服务实现  - 为订单服务提供价格查询功能

 */


@Component
@DubboService(group = "rpc")
@Slf4j
public class MerchantDubboServiceImpl implements MerchantDubboService {


    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;
    @Autowired
    private CourtMapper courtMapper;

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private VenueBookingSlotTemplateMapper slotTemplateMapper;

    @Autowired
    private VenueBookingSlotRecordMapper slotRecordMapper;

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Autowired
    private VenueFacilityRelationMapper venueFacilityRelationMapper;

    @Autowired
    private VenueActivityMapper venueActivityMapper;

    @Autowired
    private ActivityTypeMapper activityTypeMapper;

    @Autowired
    private IVenueActivityParticipantService participantService;

    @Autowired
    private IVenueBookingService venueBookingService;

    @Value("${default_image.venue_cover}")
    private String defaultVenueCoverImage;
    @Autowired
    private VenueBookingSlotRecordMapper venueBookingSlotRecordMapper;


    @Override
    public PricingResultDto quoteVenue(PricingRequestDto dto) {
        if (dto == null || dto.getSlotIds() == null || dto.getSlotIds().isEmpty()) {
            log.warn("请求参数不合法");
            throw new GloboxApplicationException("预订槽位信息不能为空");
        }
        List<VenueBookingSlotTemplate> templates = slotTemplateMapper.selectBatchIds(dto.getSlotIds());
        if (templates.size() != dto.getSlotIds().size()) {
            log.warn("部分槽位模板不存在 - 请求{}个，找到{}个", dto.getSlotIds().size(), templates.size());
            throw new GloboxApplicationException("部分槽位不存在");
        }

        // 第一次验证（获取锁前）- 快速失败，无事务环境
        validateSlots(dto.getSlotIds(), dto.getBookingDate());

        List<Long> courtIds = templates.stream()
                .map(VenueBookingSlotTemplate::getCourtId)
                .distinct()
                .collect(Collectors.toList());

        List<Court> courts = courtMapper.selectBatchIds(courtIds);
        if (courts.isEmpty()) {
            log.warn("场地不存在: courtIds={}", courtIds);
            throw new GloboxApplicationException("场地信息不存在");
        }
        Long venueId = courts.get(0).getVenueId();
        boolean allSameVenue = courts.stream()
                .allMatch(court -> court.getVenueId().equals(venueId));
        if (!allSameVenue) {
            log.warn("所选槽位来自不同场馆，不允许 - userId: {}", dto.getUserId());
            throw new GloboxApplicationException("所选槽位必须来自同一场馆");
        }
        Venue venue = venueMapper.selectById(venueId);
        if (venue == null) {
            log.warn("场馆不存在: venueId={}", venueId);
            throw new GloboxApplicationException("场馆信息不存在");
        }

        Map<Long, String> courtNameMap = courts.stream()
                .collect(Collectors.toMap(Court::getCourtId, Court::getName));

        List<String> lockKeys = dto.getSlotIds().stream()
                .map(slotId -> buildLockKey(slotId, dto.getBookingDate()))
                .collect(Collectors.toList());

        List<RLock> locks = null;

        try {
            // 批量获取所有槽位的锁（逐个加锁）
            locks = redisDistributedLock.tryLockMultiple(lockKeys, 1, -1L, TimeUnit.SECONDS);
            if (locks == null) {
                log.warn("获取分布式锁失败 - userId: {}, slotTemplateIds: {}", dto.getUserId(), dto.getSlotIds());
                throw new GloboxApplicationException("槽位正在被其他用户预订，请稍后重试");
            }
            log.info("【锁包事务】成功获取分布式锁 - userId: {}, 槽位数: {}", dto.getUserId(), dto.getSlotIds().size());

            // 调用事务Service执行预订和计价逻辑
            // 事务在此时才开启，确保能读取到其他已提交事务的最新数据
            // 使用Lambda表达式创建回调，复用validateSlots方法
            PricingResultDto result = venueBookingService.executeBookingInTransaction(
                    dto,
                    templates,
                    venue,
                    courtNameMap,
                    () -> validateSlots(dto.getSlotIds(), dto.getBookingDate()));

            log.info("事务执行成功 - userId: {}", dto.getUserId());

            return result;

        } finally {
            // 释放锁（无论事务成功还是失败）
            if (locks != null) {
                redisDistributedLock.unlockMultiple(locks);
                log.info("【锁包事务】释放分布式锁 - userId: {}", dto.getUserId());
            }
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PricingActivityResultDto quoteVenueActivity(PricingActivityRequestDto dto) {
        // 参数验证
        if (dto == null || dto.getActivityId() == null || dto.getUserId() == null) {
            log.warn("活动报名请求参数不合法");
            throw new GloboxApplicationException("活动ID和用户ID不能为空");
        }

        // 查询活动详情
        VenueActivity activity = venueActivityMapper.selectById(dto.getActivityId());
        if (activity == null) {
            log.warn("活动不存在 - activityId={}", dto.getActivityId());
            throw new GloboxApplicationException("活动不存在");
        }

        // 验证活动是否有效（报名期限未到期）
        LocalDateTime now = LocalDateTime.now();
        if (activity.getRegistrationDeadline() != null && now.isAfter(activity.getRegistrationDeadline())) {
            log.warn("活动报名已截止 - activityId={}", dto.getActivityId());
            throw new GloboxApplicationException("活动报名已截止");
        }

        // 查询场馆和场地信息
        Venue venue = venueMapper.selectById(activity.getVenueId());
        if (venue == null) {
            log.warn("场馆不存在 - venueId={}", activity.getVenueId());
            throw new GloboxApplicationException("场馆信息不存在");
        }

        Court court = courtMapper.selectById(activity.getCourtId());
        if (court == null) {
            log.warn("场地不存在 - courtId={}", activity.getCourtId());
            throw new GloboxApplicationException("场地信息不存在");
        }

        // 执行用户报名（原子操作，防止超卖）
        VenueActivityParticipant participant = participantService.registerUserToActivity(
                dto.getActivityId(),
                dto.getUserId()
        );
        log.info("用户活动报名成功 - activityId: {}, userId: {}, participantId: {}",
                dto.getActivityId(), dto.getUserId(), participant.getParticipantId());

        // 获取活动类型信息
        ActivityType activityType = null;
        String activityTypeCode = null;
        if (activity.getActivityTypeId() != null) {
            activityType = activityTypeMapper.selectById(activity.getActivityTypeId());
            if (activityType != null) {
                activityTypeCode = activityType.getTypeCode();
            }
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
        return PricingActivityResultDto.builder()
                .recordQuote(Collections.singletonList(activityQuote))
                .orderLevelExtras(Collections.emptyList())
                .sourcePlatform(venue.getVenueType())
                .sellerName(venue.getName())
                .sellerId(activity.getVenueId())
                .bookingDate(activity.getActivityDate())
                .activityId(activity.getActivityId())
                .activityTypeCode(activityTypeCode)
                .activityTypeName(activity.getActivityTypeDesc())
                .build();
    }

    @Override
    public VenueSnapshotResultDto getVenueSnapshot(VenueSnapshotRequestDto dto) {

        // 查询场馆信息
        Venue venue = venueMapper.selectById(dto.getVenueId());
        if (venue == null) {
            log.warn("场馆不存在: venueId={}", dto.getVenueId());
            throw new GloboxApplicationException("场馆信息不存在");
        }
        //  查询场地信息
        List<Court> courts = courtMapper.selectBatchIds(dto.getCourtId());
        if (courts.isEmpty()) {
            throw new GloboxApplicationException("场地信息不存在");
        }

        // 计算距离
        BigDecimal distance = BigDecimal.ZERO;
        if (venue.getLatitude() != null && venue.getLongitude() != null) {
            distance = DistanceUtils.calculateDistance(
                    dto.getLatitude(),
                    dto.getLongitude(),
                    venue.getLatitude().doubleValue(),
                    venue.getLongitude().doubleValue()
            );
        }

        // 获取封面图片
        String coverImage = defaultVenueCoverImage;
        if (venue.getImageUrls() != null && !venue.getImageUrls().isEmpty()) {
            String[] imageUrls = venue.getImageUrls().split(";");
            if (imageUrls.length > 0 && !imageUrls[0].trim().isEmpty()) {
                coverImage = imageUrls[0].trim();
            }
        }
        // 查询便利设施 - 从 venue_facility_relation
        List<String> facilities = venueFacilityRelationMapper.selectList(
                new LambdaQueryWrapper<VenueFacilityRelation>()
                        .eq(VenueFacilityRelation::getVenueId, dto.getVenueId())
        ).stream()
                .map(VenueFacilityRelation::getFacilityName)
                .collect(Collectors.toList());
        // 构建场地快照列表
        List<VenueSnapshotResultDto.CourtSnapshotDto> courtSnapshots = courts.stream()
                .map(court -> VenueSnapshotResultDto.CourtSnapshotDto.builder()
                        .id(court.getCourtId())
                        .name(court.getName())
                        .groundType(court.getGroundType())
                        .courtType(court.getCourtType())
                        .build())
                .collect(Collectors.toList());

        // 构建返回结果
        return VenueSnapshotResultDto.builder()
                .id(venue.getVenueId())
                .name(venue.getName())
                .phone(venue.getPhone())
                .region(venue.getRegion())
                .address(venue.getAddress())
                .coverImage(coverImage)
                .distance(distance)
                .facilities(facilities)
                .courtSnapshotDtos(courtSnapshots)
                .build();
    }


    /**
     * 验证槽位是否可用
     * 直接查询是否存在状态不合法的记录，如果有任何不可用的槽位则抛异常
     *
     * @param slotTemplateIds 请求的槽位模板ID列表
     * @param bookingDate 预订日期
     */
    private void validateSlots(List<Long> slotTemplateIds, LocalDate bookingDate) {
        if (slotTemplateIds == null || slotTemplateIds.isEmpty()) {
            return;
        }

        // 直接查询是否存在状态不是AVAILABLE的记录
        LambdaQueryWrapper<VenueBookingSlotRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(VenueBookingSlotRecord::getSlotTemplateId, slotTemplateIds)
                .eq(VenueBookingSlotRecord::getBookingDate, bookingDate.atStartOfDay())
                .ne(VenueBookingSlotRecord::getStatus, BookingSlotStatus.AVAILABLE.getValue());
        Long count = slotRecordMapper.selectCount(queryWrapper);
        log.error("count{}",count);
        List<VenueBookingSlotRecord> venueBookingSlotRecords = venueBookingSlotRecordMapper.selectList(new LambdaQueryWrapper<VenueBookingSlotRecord>()
                .in(VenueBookingSlotRecord::getSlotTemplateId, List.of(5001438L, 5001435L, 5001432L)));
        log.info("查询到的venueBookingSlotRecords{}",venueBookingSlotRecords);
        // 如果存在任何状态不合法的记录，说明有槽位不可用
        if (count > 0) {
            log.warn("检测到{}个不可用的槽位", count);
            throw new GloboxApplicationException("部分槽位不可用或已被占用，请重新选择");
        }

        log.debug("槽位验证通过 - 所有{}个槽位可用", slotTemplateIds.size());
    }


    /**
     * 构建锁键
     */
    private String buildLockKey(Long slotTemplateId, LocalDate bookingDate) {
        return BookingCacheConstants.BOOKING_LOCK_KEY_PREFIX + slotTemplateId + BookingCacheConstants.BOOKING_LOCK_KEY_SEPARATOR + bookingDate;
    }

}
