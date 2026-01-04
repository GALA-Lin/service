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
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplate;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplateMapper;
import com.unlimited.sports.globox.venue.mapper.VenueActivityMapper;
import com.unlimited.sports.globox.venue.mapper.ActivityTypeMapper;
import com.unlimited.sports.globox.model.venue.entity.venues.ActivityType;
import com.unlimited.sports.globox.venue.service.IVenueBookingSlotRecordService;
import com.unlimited.sports.globox.venue.service.IVenueActivityService;
import com.unlimited.sports.globox.venue.service.IVenueActivityParticipantService;
import com.unlimited.sports.globox.venue.service.VenuePriceService;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivityParticipant;
import com.unlimited.sports.globox.common.utils.DistanceUtils;
import com.unlimited.sports.globox.merchant.mapper.VenueFacilityRelationMapper;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueFacilityRelation;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
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
    private CourtMapper courtMapper;

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private VenueBookingSlotTemplateMapper slotTemplateMapper;

    @Autowired
    private VenueBookingSlotRecordMapper slotRecordMapper;

    @Autowired
    private VenuePriceService venuePriceService;

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Autowired
    private IVenueBookingSlotRecordService slotRecordService;

    @Autowired
    private VenuePriceTemplateMapper venuePriceTemplateMapper;

    @Autowired
    private VenueFacilityRelationMapper venueFacilityRelationMapper;

    @Autowired
    private VenueActivityMapper venueActivityMapper;

    @Autowired
    private ActivityTypeMapper activityTypeMapper;

    @Autowired
    private IVenueActivityParticipantService participantService;

    @Value("${default_image.venue_cover}")
    private String defaultVenueCoverImage;


    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
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

        List<VenueBookingSlotTemplate> availableTemplates = validateAndGetSlots(dto.getSlotIds(), dto.getBookingDate());
        if (availableTemplates.size() != dto.getSlotIds().size()) {
            log.warn("部分槽位不可用 - 请求{}个，可用{}个", dto.getSlotIds().size(), availableTemplates.size());
            throw new GloboxApplicationException("部分槽位不可用或已被占用，请重新选择");
        }

        templates = availableTemplates;

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

        RLock multiLock = null;
        List<VenueBookingSlotRecord> records = null;

        try {
            // 使用Redisson MultiLock批量获取所有槽位的锁
            // waitTime=1秒：等待锁的超时时间
            // leaseTime=30秒：锁的持有时间，确保在业务逻辑执行期间保持有效
            multiLock = redisDistributedLock.tryLockMultiple(lockKeys, 1, 30, TimeUnit.SECONDS);
            if (multiLock == null) {
                log.warn("获取分布式锁失败 - userId: {}, slotTemplateIds: {}", dto.getUserId(), dto.getSlotIds());
                throw new GloboxApplicationException("槽位正在被其他用户预订，请稍后重试");
            }
            log.debug("成功获取分布式锁（MultiLock） - userId: {}, 槽位数: {}", dto.getUserId(), dto.getSlotIds().size());

            // 二次验证
            List<VenueBookingSlotTemplate> finalTemplates = validateAndGetSlots(dto.getSlotIds(), dto.getBookingDate());
            if (finalTemplates.size() != dto.getSlotIds().size()) {
                log.warn("锁内二次验证失败 - 槽位已被其他用户占用");
                throw new GloboxApplicationException("槽位已被其他用户抢先预订，请重新选择");
            }

            // 原子性地更新槽位状态，防止超卖
            records = lockBookingSlots(finalTemplates, dto.getUserId(), dto.getBookingDate());
            if (records == null) {
                // lockBookingSlots返回null表示槽位已被其他用户占用（UPDATE失败）
                log.warn("占用槽位失败 - 槽位已被其他用户占用 userId: {}", dto.getUserId());
                throw new GloboxApplicationException("槽位已被其他用户占用，请重新选择");
            }
            log.debug("槽位占用成功，recordIds: {}", records.stream()
                    .map(VenueBookingSlotRecord::getBookingSlotRecordId)
                    .collect(Collectors.toList()));

        } finally {
            if (multiLock != null) {
                redisDistributedLock.unlockMultiple(multiLock);
                log.info("释放分布式锁 - userId: {}", dto.getUserId());
            }
        }
        //  获取并验证价格模板
        VenuePriceTemplate priceTemplate = venuePriceTemplateMapper.selectById(venue.getTemplateId());
        if (priceTemplate == null) {
            log.warn("价格模板不存在: templateId={}", venue.getTemplateId());
            throw new GloboxApplicationException("场馆价格模板未配置");
        }
        if (!priceTemplate.getIsEnabled()) {
            log.warn("价格模板未启用: templateId={}", priceTemplate.getTemplateId());
            throw new GloboxApplicationException("场馆价格模板未启用");
        }

        // 计算槽位价格
        List<RecordQuote> recordQuotes = venuePriceService.calculateSlotQuotes(
                records,
                venueId,
                venue.getName(),
                dto.getBookingDate(),
                courtNameMap
        );
        if (recordQuotes.isEmpty()) {
            log.warn("无法计算任何槽位的价格");
            throw new GloboxApplicationException("无法计算槽位价格");
        }

        BigDecimal totalBasePrice = recordQuotes.stream()
                .map(RecordQuote::getUnitPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        // 计算额外费用
        List<OrderLevelExtraQuote> extraQuotes = venuePriceService.calculateExtraCharges(
                venueId,
                totalBasePrice,
                templates.size()
        );

        // 构建返回结果
        return PricingResultDto.builder()
                .recordQuote(recordQuotes)
                .orderLevelExtras(extraQuotes)
                .sourcePlatform(venue.getVenueType())
                .sellerName(venue.getName())
                .sellerId(venueId)
                .bookingDate(dto.getBookingDate())
                .build();
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
     * 验证并获取槽位信息
     *
     * @param slotTemplateIds 请求的槽位模板ID列表
     * @param bookingDate 预订日期
     * @return 通过验证的槽位模板列表
     */
    private List<VenueBookingSlotTemplate> validateAndGetSlots(List<Long> slotTemplateIds, LocalDate bookingDate) {
        if (slotTemplateIds == null || slotTemplateIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询槽位模板
        List<VenueBookingSlotTemplate> templates = slotTemplateMapper.selectBatchIds(slotTemplateIds);

        // 批量查询槽位记录
        List<Long> templateIds = templates.stream()
                .map(VenueBookingSlotTemplate::getBookingSlotTemplateId)
                .collect(Collectors.toList());

        List<VenueBookingSlotRecord> records = slotRecordMapper.selectByTemplateIdsAndDate(
                templateIds,
                bookingDate
        );

        // 构建记录映射
        Map<Long, VenueBookingSlotRecord> recordMap = records.stream()
                .collect(Collectors.toMap(VenueBookingSlotRecord::getSlotTemplateId, record -> record));

        log.debug("批量查询槽位记录 - 返回数: {}", records.size());

        // 过滤可用的槽位（没有记录或状态为AVAILABLE）
        List<VenueBookingSlotTemplate> validTemplates = templates.stream()
                .filter(template -> {
                    VenueBookingSlotRecord record = recordMap.get(template.getBookingSlotTemplateId());
                    boolean isAvailable;

                    if (record == null) {
                        // 没有记录，表示未被占用，可用
                        isAvailable = true;
                    } else {
                        // 检查状态
                        try {
                            isAvailable = BookingSlotStatus.fromValue(record.getStatus()) == BookingSlotStatus.AVAILABLE;
                            if (!isAvailable) {
                                log.debug("槽位不可用 - slotTemplateId: {}, status: {}",
                                        template.getBookingSlotTemplateId(), record.getStatus());
                            }
                        } catch (IllegalArgumentException e) {
                            log.warn("槽位状态值非法 - slotTemplateId: {}, status: {}",
                                    template.getBookingSlotTemplateId(), record.getStatus());
                            isAvailable = false;
                        }
                    }
                    return isAvailable;
                })
                .collect(Collectors.toList());

        return validTemplates;
    }

    /**
     * 占用预订槽位
     * 将槽位状态改为LOCKED_IN，并记录操作人信息
     *
     * @return 返回占用的槽位记录列表（按templates顺序）
     */
    private List<VenueBookingSlotRecord> lockBookingSlots(List<VenueBookingSlotTemplate> templates, Long userId, LocalDate bookingDate) {
        log.info("开始占用槽位 - userId: {}, 槽位数: {}", userId, templates.size());

        if (templates.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取所有模板ID
        List<Long> templateIds = templates.stream()
                .map(VenueBookingSlotTemplate::getBookingSlotTemplateId)
                .collect(Collectors.toList());

        // 批量查询已存在的记录
        List<VenueBookingSlotRecord> existingRecords = slotRecordMapper.selectByTemplateIdsAndDate(
                templateIds,
                bookingDate
        );

        // 构建已有记录的ID集合
        Set<Long> existingTemplateIds = existingRecords.stream()
                .map(VenueBookingSlotRecord::getSlotTemplateId)
                .collect(Collectors.toSet());

        // 保持 template 和 record 的对应关系
        Map<Long, VenueBookingSlotRecord> templateToRecordMap = new LinkedHashMap<>();
        List<VenueBookingSlotRecord> toInsert = new ArrayList<>();
        List<VenueBookingSlotRecord> toUpdate = new ArrayList<>();

        for (VenueBookingSlotTemplate template : templates) {
            if (existingTemplateIds.contains(template.getBookingSlotTemplateId())) {
                // 已有记录 - 更新状态和操作人信息
                VenueBookingSlotRecord record = existingRecords.stream()
                        .filter(r -> r.getSlotTemplateId().equals(template.getBookingSlotTemplateId()))
                        .findFirst()
                        .orElse(null);
                if (record != null) {
                    record.setStatus(BookingSlotStatus.LOCKED_IN.getValue());
                    record.setOperatorId(userId);
                    record.setOperatorSource(OperatorSourceEnum.USER);
                    toUpdate.add(record);
                    templateToRecordMap.put(template.getBookingSlotTemplateId(), record);
                }
            } else {
                // 新记录 - 创建并设置操作人信息
                VenueBookingSlotRecord record = new VenueBookingSlotRecord();
                record.setSlotTemplateId(template.getBookingSlotTemplateId());
                record.setBookingDate(bookingDate.atStartOfDay());
                record.setStatus(BookingSlotStatus.LOCKED_IN.getValue());
                record.setOperatorId(userId);
                record.setOperatorSource(OperatorSourceEnum.USER);
                toInsert.add(record);
                templateToRecordMap.put(template.getBookingSlotTemplateId(), record);
            }
        }

        // 批量insert
        if (!toInsert.isEmpty()) {
            slotRecordService.saveBatch(toInsert);
        }

        // 批量update - 使用原子性更新，只有当前状态为AVAILABLE时才能更新
        // 如果返回0表示槽位已被其他用户占用（超卖防护）
        int successUpdateCount = 0;
        for (VenueBookingSlotRecord record : toUpdate) {
            int affectedRows = slotRecordMapper.updateStatusIfAvailable(
                    record.getBookingSlotRecordId(),
                    BookingSlotStatus.LOCKED_IN.getValue(),
                    userId
            );
            if (affectedRows == 0) {
                // 该槽位已被其他用户占用，这不应该发生（因为有Redis锁）
                // 但如果发生了，说明另一个用户的事务还没提交，等待片刻后重试或直接返回错误
                log.warn("槽位已被其他用户占用 - recordId: {}, slotTemplateId: {}",
                        record.getBookingSlotRecordId(), record.getSlotTemplateId());
                return null;  // 返回null表示占用失败
            }
            successUpdateCount++;
        }

        // 按照 templates 的顺序返回 records，确保顺序正确
        List<VenueBookingSlotRecord> records = templates.stream()
                .map(template -> templateToRecordMap.get(template.getBookingSlotTemplateId()))
                .collect(Collectors.toList());

        log.info("槽位占用完成 - userId: {}, 槽位数: {}, 新增: {}, 更新: {} (成功: {}), recordIds: {}", userId, templates.size(),
                toInsert.size(), toUpdate.size(), successUpdateCount,
                records.stream().map(VenueBookingSlotRecord::getBookingSlotRecordId).collect(Collectors.toList()));

        return records;
    }

    /**
     * 构建锁键
     */
    private String buildLockKey(Long slotTemplateId, LocalDate bookingDate) {
        return BookingCacheConstants.BOOKING_LOCK_KEY_PREFIX + slotTemplateId + BookingCacheConstants.BOOKING_LOCK_KEY_SEPARATOR + bookingDate;
    }

}
