package com.unlimited.sports.globox.venue.dubbo;

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
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplateMapper;
import com.unlimited.sports.globox.venue.service.IVenueBookingSlotRecordService;
import com.unlimited.sports.globox.venue.service.VenuePriceService;
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

    @Value("${default_image.venue_cover}")
    private String defaultVenueCoverImage;


    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PricingResultDto quoteVenue(PricingRequestDto dto) {
        try {
            if (dto == null || dto.getSlotIds() == null || dto.getSlotIds().isEmpty()) {
                log.warn("请求参数不合法");
                return buildErrorResult("预订槽位信息不能为空");
            }
            List<VenueBookingSlotTemplate> templates = slotTemplateMapper.selectBatchIds(dto.getSlotIds());
            if (templates.size() != dto.getSlotIds().size()) {
                log.warn("部分槽位模板不存在 - 请求{}个，找到{}个", dto.getSlotIds().size(), templates.size());
                return buildErrorResult("部分槽位不存在");
            }

            List<VenueBookingSlotTemplate> availableTemplates = validateAndGetSlots(dto.getSlotIds(), dto.getBookingDate());
            if (availableTemplates.size() != dto.getSlotIds().size()) {
                log.warn("部分槽位不可用 - 请求{}个，可用{}个", dto.getSlotIds().size(), availableTemplates.size());
                return buildErrorResult("部分槽位不可用或已被占用，请重新选择");
            }

            templates = availableTemplates;

            List<Long> courtIds = templates.stream()
                    .map(VenueBookingSlotTemplate::getCourtId)
                    .distinct()
                    .collect(Collectors.toList());

            List<Court> courts = courtMapper.selectBatchIds(courtIds);
            if (courts.isEmpty()) {
                log.warn("场地不存在: courtIds={}", courtIds);
                return buildErrorResult("场地信息不存在");
            }
            Long venueId = courts.get(0).getVenueId();
            boolean allSameVenue = courts.stream()
                    .allMatch(court -> court.getVenueId().equals(venueId));
            if (!allSameVenue) {
                log.warn("所选槽位来自不同场馆，不允许 - userId: {}", dto.getUserId());
                return buildErrorResult("所选槽位必须来自同一场馆");
            }
            Venue venue = venueMapper.selectById(venueId);
            if (venue == null) {
                log.warn("场馆不存在: venueId={}", venueId);
                return buildErrorResult("场馆信息不存在");
            }

            Map<Long, String> courtNameMap = courts.stream()
                    .collect(Collectors.toMap(Court::getCourtId, Court::getName));


            List<String> lockKeys = dto.getSlotIds().stream()
                    .map(slotId -> buildLockKey(slotId, dto.getBookingDate()))
                    .collect(Collectors.toList());

            List<RLock> locks = null;

            try {
                // 利用Redisson内置重试机制
                locks = redisDistributedLock.tryLockMultiple(lockKeys, 2, 10, TimeUnit.SECONDS);
                if (locks == null) {
                    log.warn("获取分布式锁失败 - userId: {}, slotTemplateIds: {}", dto.getUserId(), dto.getSlotIds());
                    return buildErrorResult("槽位正在被其他用户预订，请稍后重试");
                }
                log.debug("成功获取分布式锁 - userId: {}, 锁数: {}", dto.getUserId(), locks.size());

                // 二次验证
                List<VenueBookingSlotTemplate> finalTemplates = validateAndGetSlots(dto.getSlotIds(), dto.getBookingDate());
                if (finalTemplates.size() != dto.getSlotIds().size()) {
                    log.warn("锁内二次验证失败 - 槽位已被其他用户占用");
                    return buildErrorResult("槽位已被其他用户抢先预订，请重新选择");
                }
                lockBookingSlots(finalTemplates, dto.getUserId(), dto.getBookingDate());

            } finally {
                if (locks != null) {
                    redisDistributedLock.unlockMultiple(locks);
                    log.info("释放分布式锁 - userId: {}, 锁数: {}", dto.getUserId(), locks.size());
                }

            }
            //  获取并验证价格模板
            VenuePriceTemplate priceTemplate = venuePriceTemplateMapper.selectById(venue.getTemplateId());
            if (priceTemplate == null) {
                log.warn("价格模板不存在: templateId={}", venue.getTemplateId());
                return buildErrorResult("场馆价格模板未配置");
            }
            if (!priceTemplate.getIsEnabled()) {
                log.warn("价格模板未启用: templateId={}", priceTemplate.getTemplateId());
                return buildErrorResult("场馆价格模板未启用");
            }

            // 计算槽位价格
            List<RecordQuote> recordQuotes = venuePriceService.calculateSlotQuotes(
                    templates,
                    venueId,
                    venue.getName(),
                    dto.getBookingDate(),
                    courtNameMap
            );
            if (recordQuotes.isEmpty()) {
                log.warn("无法计算任何槽位的价格");
                return buildErrorResult("无法计算槽位价格");
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
                    .build();

        } catch (Exception e) {
            return buildErrorResult("订场失败: " + e.getMessage());
        }
    }

    @Override
    public PricingActivityResultDto quoteVenueActivity(PricingActivityRequestDto dto) {
        return null;
    }

    @Override
    public VenueSnapshotResultDto getVenueSnapshot(VenueSnapshotRequestDto dto) {

        // 查询场馆信息
        Venue venue = venueMapper.selectById(dto.getVenueId());
        if (venue == null) {
            log.warn("场馆不存在: venueId={}", dto.getVenueId());
            throw new IllegalArgumentException("场馆信息不存在");
        }
        //  查询场地信息
        List<Court> courts = courtMapper.selectBatchIds(dto.getCourtId());
        if (courts.isEmpty()) {
            throw new IllegalArgumentException("场地信息不存在");
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
     */
    private void lockBookingSlots(List<VenueBookingSlotTemplate> templates, Long userId, LocalDate bookingDate) {
        log.info("开始占用槽位 - userId: {}, 槽位数: {}", userId, templates.size());

        if (templates.isEmpty()) {
            return;
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

        // 分离需要insert和update的记录
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
            }
        }

        // 批量insert
        if (!toInsert.isEmpty()) {
            slotRecordService.saveBatch(toInsert);
        }

        // 批量update
        if (!toUpdate.isEmpty()) {
            slotRecordService.updateBatchById(toUpdate);
        }

        log.info("槽位占用完成 - userId: {}, 槽位数: {}, 新增: {}, 更新: {}", userId, templates.size(),
                toInsert.size(), toUpdate.size());
    }

    /**
     * 构建锁键
     */
    private String buildLockKey(Long slotTemplateId, LocalDate bookingDate) {
        return BookingCacheConstants.BOOKING_LOCK_KEY_PREFIX + slotTemplateId + BookingCacheConstants.BOOKING_LOCK_KEY_SEPARATOR + bookingDate;
    }

    /**
     * 构建错误结果
     */
    private PricingResultDto buildErrorResult(String message) {
        log.error("价格查询失败: {}", message);
        return PricingResultDto.builder()
                .recordQuote(Collections.emptyList())
                .orderLevelExtras(Collections.emptyList())
                .sellerName("")
                .sellerId(0L)
                .sourcePlatform(1)
                .build();
    }
}
