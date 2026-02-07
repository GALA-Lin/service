package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.enums.order.ChargeModeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.VenueCode;
import com.unlimited.sports.globox.dubbo.merchant.dto.*;
import com.unlimited.sports.globox.model.merchant.enums.VenueTypeEnum;
import com.unlimited.sports.globox.model.venue.dto.*;
import com.unlimited.sports.globox.model.venue.enums.*;
import com.unlimited.sports.globox.model.venue.vo.*;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueFacilityRelationMapper;
import com.unlimited.sports.globox.venue.adapter.constant.AwayVenueCacheConstants;
import com.unlimited.sports.globox.venue.adapter.dto.*;
import com.unlimited.sports.globox.venue.config.AwayConfig;
import com.unlimited.sports.globox.venue.dto.ActivityPreviewContext;
import com.unlimited.sports.globox.venue.dto.SlotBookingContext;
import com.unlimited.sports.globox.venue.mapper.VenueActivityMapper;
import com.unlimited.sports.globox.venue.mapper.ActivityTypeMapper;
import com.unlimited.sports.globox.venue.adapter.ThirdPartyPlatformAdapter;
import com.unlimited.sports.globox.venue.adapter.ThirdPartyPlatformAdapterFactory;
import com.unlimited.sports.globox.venue.mapper.VenueThirdPartyConfigMapper;
import com.unlimited.sports.globox.venue.mapper.ThirdPartyPlatformMapper;
import com.unlimited.sports.globox.venue.mapper.VenueActivityParticipantMapper;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueThirdPartyConfig;
import com.unlimited.sports.globox.model.venue.entity.venues.ThirdPartyPlatform;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivityParticipant;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivityParticipantStatusEnum;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.merchant.entity.VenueBusinessHours;
import com.unlimited.sports.globox.model.merchant.enums.BusinessHourRuleTypeEnum;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotRecord;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueFacilityRelation;
import com.unlimited.sports.globox.model.venue.entity.venues.ActivityType;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotRecordMapper;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotTemplateMapper;
import com.unlimited.sports.globox.venue.service.*;
import com.unlimited.sports.globox.venue.util.TimeSlotSplitUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BookingServiceImpl implements IBookingService {


    @Autowired
    private VenueBookingSlotRecordMapper slotRecordMapper;

    @Autowired
    private IVenueBookingSlotRecordService slotRecordService;

    @Autowired
    private IVenuePriceService venuePriceService;

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private CourtMapper courtMapper;

    @Autowired
    private VenueBookingSlotTemplateMapper slotTemplateMapper;

    @Autowired
    private IVenueActivityService venueActivityService;

    @Autowired
    private IVenueBusinessHoursService venueBusinessHoursService;

    @Autowired
    private VenueFacilityRelationMapper venueFacilityRelationMapper;

    @Autowired
    private VenueActivityMapper venueActivityMapper;

    @Autowired
    private ActivityTypeMapper activityTypeMapper;

    @Autowired
    private VenueThirdPartyConfigMapper venueThirdPartyConfigMapper;

    @Autowired
    private VenueActivityParticipantMapper venueActivityParticipantMapper;

    @Autowired
    private VenuePriceServiceImpl venuePriceServiceImpl;

    @Autowired
    private ThirdPartyPlatformMapper thirdPartyPlatformMapper;

    @Autowired
    private ThirdPartyPlatformAdapterFactory adapterFactory;

    @Value("${default_image.venue_cover}")
    private String defaultVenueCoverImage;


    @Autowired
    private AwayConfig awayConfig;
    /**
     * 获取场馆指定日期所有场地的槽位占用情况
     * 基于槽位模板和实际预订记录返回数据
     * 支持价格计算（基于价格模板和价格覆盖表）
     *
     * @param dto 查询条件，包含场馆ID和预订日期
     * @return 场地列表，每个场地包含其所有时间槽位的占用状态和价格
     */
    @Override
    public List<CourtSlotVo> getCourtSlots(GetCourtSlotsDto dto) {
        log.info("获取场馆槽位信息: venueId={}, bookingDate={}=", dto.getVenueId(), dto.getBookingDate());

        // 验证场馆存在
        Venue venue = venueMapper.selectById(dto.getVenueId());
        if (venue == null) {
            throw new GloboxApplicationException(VenueCode.VENUE_NOT_EXIST);
        }

        // 时间控制：检查是否允许查看该日期的槽位
        if (!venue.validSlotVisibilityPermission( dto.getBookingDate())) {
            log.debug("时间未到,不允许查看");
            return Collections.emptyList();
        }

        // 判断是否是Away球场（第三方平台）
        if (venue.getVenueType() != null && venue.getVenueType().equals(VenueTypeEnum.AWAY.getCode())) {
            log.info("获取Away球场槽位 - venueId: {}, bookingDate: {}", dto.getVenueId(), dto.getBookingDate());
            return getAwayCourtSlots(dto.getVenueId(), dto.getBookingDate());
        }

        // 查询所有开放的场地
        List<Court> courts = courtMapper.selectList(
                new LambdaQueryWrapper<Court>()
                        .eq(Court::getVenueId, dto.getVenueId())
                        .eq(Court::getStatus, CourtStatus.OPEN.getValue())
                        .orderByAsc(Court::getCourtId)
        );

        if (courts.isEmpty()) {
            log.warn("场馆没有开放的场地: venueId={}", dto.getVenueId());
            return Collections.emptyList();
        }

        // 获取营业时间配置
        VenueBusinessHours businessHours = venueBusinessHoursService.getBusinessHoursByDate(dto.getVenueId(),
                dto.getBookingDate());
        if (businessHours == null) {
            log.warn("场馆未配置营业时间: venueId={}, bookingDate={}", dto.getVenueId(), dto.getBookingDate());
            return Collections.emptyList();
        }
        if(BusinessHourRuleTypeEnum.CLOSED_DATE.getCode().equals(businessHours.getRuleType())) {
            // 当天不开放
            return Collections.emptyList();
        }
        LocalDate bookingDate = dto.getBookingDate();
        LocalTime openTime = businessHours.getOpenTime();
        LocalTime closeTime = businessHours.getCloseTime();
        List<Long> courtIds = courts.stream().map(Court::getCourtId).toList();

        // 批量查询所有场地的槽位模板
        List<VenueBookingSlotTemplate> allTemplates = slotTemplateMapper.selectByCourtIdsAndTimeRange(
                courtIds,
                openTime,
                closeTime
        );

        // 批量查询所有场地该日期的槽位记录
        List<VenueBookingSlotRecord> allRecords = slotRecordMapper.selectByCourtIdsAndDate(
                courtIds,
                bookingDate
        );

        // 构建映射表
        Map<Long, List<VenueBookingSlotTemplate>> templatesByCourtId = allTemplates.stream()
                .collect(Collectors.groupingBy(VenueBookingSlotTemplate::getCourtId));

        Map<Long, VenueBookingSlotRecord> recordMap = allRecords.stream()
                .collect(Collectors.toMap(VenueBookingSlotRecord::getSlotTemplateId, record -> record));

        // 计算完整价格 - 使用获取按场地分组的价格
        Map<Long, Court> courtMap = courts.stream()
                .collect(Collectors.toMap(Court::getCourtId, court -> court));

        Map<Long, Map<LocalTime, BigDecimal>> pricesByCourtId = venuePriceServiceImpl.calculateSlotPricesByCourtTemplates(
                allTemplates,
                venue.getVenueId(),
                bookingDate,
                courtMap
        );

        log.debug("批量获取价格完成 - 场地数: {}", pricesByCourtId.size());

        // 批量获取场馆在该日期的所有活动
        List<VenueActivity> allActivities = venueActivityService.getActivitiesByVenueAndDate(
                venue.getVenueId(),
                bookingDate
        );

        // 构建活动ID映射表，用于快速查询活动详情：活动ID -> 活动详情
        Map<Long, VenueActivity> activityMap = allActivities.stream()
                .collect(Collectors.toMap(VenueActivity::getActivityId, activity -> activity));

        // 批量获取所有活动占用的槽位映射（槽位模板ID -> 活动ID）
        List<Long> allActivityIds = allActivities.stream()
                .map(VenueActivity::getActivityId)
                .collect(Collectors.toList());

        Map<Long, Long> activityLockedSlots = allActivityIds.isEmpty() ?
                Map.of() :
                venueActivityService.getActivityLockedSlotsByIds(allActivityIds, bookingDate);

        // 批量查询用户报名的活动（活动ID集合，只查询未取消的记录）
        Set<Long> userRegisteredActivityIds = Collections.emptySet();
        if (dto.getUserId() != null && !allActivityIds.isEmpty()) {
            List<VenueActivityParticipant> participants = venueActivityParticipantMapper.selectList(
                    new LambdaQueryWrapper<VenueActivityParticipant>()
                            .eq(VenueActivityParticipant::getUserId, dto.getUserId())
                            .in(VenueActivityParticipant::getActivityId, allActivityIds)
                            .eq(VenueActivityParticipant::getStatus, VenueActivityParticipantStatusEnum.ACTIVE.getValue())
            );
            userRegisteredActivityIds = participants.stream()
                    .map(VenueActivityParticipant::getActivityId)
                    .collect(Collectors.toSet());
        }

        // 构建场地槽位结果，过滤掉没有槽位模板的场地
        Set<Long> finalUserRegisteredActivityIds = userRegisteredActivityIds;
        return courts.stream()
                .map(court -> {
                    // 为该场地提取对应的价格
                    Map<LocalTime, BigDecimal> courtPrices = pricesByCourtId
                            .getOrDefault(court.getCourtId(), new HashMap<>());

                    return CourtSlotVo.buildVo(
                            court,
                            templatesByCourtId.getOrDefault(court.getCourtId(), Collections.emptyList()),
                            recordMap,
                            courtPrices,
                            activityMap,
                            activityLockedSlots,
                            dto.getUserId(),
                            finalUserRegisteredActivityIds
                    );
                })
                .filter(courtSlotVo -> !courtSlotVo.getSlots().isEmpty())  // 过滤掉没有槽位的场地
                .collect(Collectors.toList());
    }




    /**
     * 预订预览 - 计算价格但不锁定槽位
     * 用于在下单前展示订单信息和价格
     *
     * @param userId 用户ID
     * @param dto 预订请求参数
     * @return 包含场馆信息和价格计算结果
     */
    @Override
    public BookingPreviewResponseVo previewGeneralBooking(Long userId, GeneralBookingPreviewRequestDto dto) {
        log.info("预订预览 - userId: {}, slotIds: {}, bookingDate: {}",
                userId, dto.getSlotIds(), dto.getBookingDate());
        // 验证并准备预订上下文
        SlotBookingContext context =
                validateAndPrepareBookingContext(dto.getSlotIds(), dto.getBookingDate(), userId);
        // 提取上下文数据
        List<VenueBookingSlotTemplate> templates = context.getTemplates();
        Venue venue = context.getVenue();
        Map<Long, Court> courtMap = context.getCourtMap();
        List<Long> selectedOrderExtraIds = normalizeSelectedOrderExtraIds(dto.getSelectedOrderExtraIds());
        Map<Long, List<Long>> selectedItemExtraBySlotId =
                normalizeSelectedItemExtraBySlotId(dto.getSelectedItemExtraBySlotId());
        Set<Long> selectedOrderExtraIdSet = new HashSet<>(selectedOrderExtraIds);

        // 计算完整价格
        DetailedPricingInfo detailedPricingInfo;
        boolean isAway = VenueTypeEnum.AWAY.getCode().equals(venue.getVenueType());
        if (isAway) {
            log.info("[Away预览] 使用Away实时价格和场地数据 - venueId: {}, slotIds: {}",
                    venue.getVenueId(), dto.getSlotIds());
            // 获取第三方平台配置和适配器
            VenueThirdPartyConfig config = venueThirdPartyConfigMapper.selectOne(
                    new LambdaQueryWrapper<VenueThirdPartyConfig>()
                            .eq(VenueThirdPartyConfig::getVenueId, venue.getVenueId())
            );
            if (config == null) {
                log.error("Away球场未配置第三方平台 - venueId: {}", venue.getVenueId());
                throw new GloboxApplicationException(VenueCode.VENUE_CAN_NOT_BOOKING);
            }

            ThirdPartyPlatform platform = thirdPartyPlatformMapper.selectById(config.getThirdPartyPlatformId());
            if (platform == null) {
                log.error("第三方平台不存在 - platformId: {}", config.getThirdPartyPlatformId());
                throw new GloboxApplicationException(VenueCode.VENUE_CAN_NOT_BOOKING);
            }

            ThirdPartyPlatformAdapter adapter = adapterFactory.getAdapter(platform.getPlatformCode());

            // 调用querySlots获取实时槽位数据
            List<ThirdPartyCourtSlotDto> slotDtos = adapter.querySlots(config, dto.getBookingDate());
            if (slotDtos == null || slotDtos.isEmpty()) {
                log.error("[Away预览] 查询槽位失败 - venueId: {}, date: {}", venue.getVenueId(), dto.getBookingDate());
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            // 调用calculatePricing计算价格，获取所有槽位的详细价格列表
            List<AwaySlotPrice> awaySlotPrices = adapter.calculatePricing(config, dto.getBookingDate());
            if (awaySlotPrices == null || awaySlotPrices.isEmpty()) {
                log.error("[Away预览] 价格计算失败 - venueId: {}", venue.getVenueId());
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            // 转换为DetailedPricingInfo格式（只计算选中的槽位价格）
            detailedPricingInfo = venuePriceServiceImpl.convertAwaySlotPricesToDetailedPricingInfo(awaySlotPrices, templates, courtMap);
        } else {
            // 本地球场使用新的价格计算服务
            detailedPricingInfo = venuePriceServiceImpl.calculatePricingByCourtTemplates(
                    templates,
                    venue.getVenueId(),
                    dto.getBookingDate(),
                    courtMap
            );
        }

        // 按场地分组模板，构建BookingItemVo列表
        Map<Long, List<VenueBookingSlotTemplate>> templatesByCourtId = templates.stream()
                .collect(Collectors.groupingBy(VenueBookingSlotTemplate::getCourtId));
        Map<Long, Map<Long, Integer>> selectedItemExtraCountByCourt =
                buildSelectedItemExtraCountByCourt(templates, selectedItemExtraBySlotId);

        List<BookingItemVo> items = templatesByCourtId.entrySet().stream()
                .map(entry -> {
                    Long courtId = entry.getKey();
                    List<VenueBookingSlotTemplate> courtTemplates = entry.getValue();
                    Court court = courtMap.get(courtId);

                    if (court == null) {
                        return null;
                    }

                    // 构建场地快照
                    CourtSnapshotVo courtSnapshot = CourtSnapshotVo.builder()
                            .id(court.getCourtId())
                            .name(court.getName())
                            .groundType(court.getGroundType())
                            .courtType(court.getCourtType())
                            .build();

                    // 计算该场地的基础金额 - 统一从pricesByCourtId中获取
                    // 预览时：如果没有价格记录，返回999作为默认值；如果价格为0，保持为0
                    Map<LocalTime, BigDecimal> courtPrices = detailedPricingInfo.getPricesByCourtId()
                            .getOrDefault(courtId, new HashMap<>());
                    BigDecimal itemBaseAmount = courtTemplates.stream()
                            .map(template -> {
                                LocalTime startTime = template.getStartTime();
                                if (courtPrices.containsKey(startTime)) {
                                    // 有价格记录，使用实际价格（可能为0）
                                    return courtPrices.get(startTime);
                                } else {
                                    // 没有价格记录，返回默认值999
                                    log.warn("[预览价格] 槽位缺少价格，使用默认值999 - courtId: {}, startTime: {}",
                                            courtId, startTime);
                                    return new BigDecimal("999");
                                }
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // 构建时间段列表
                    List<SlotBookingTime> slotBookingTimes = courtTemplates.stream()
                            .map(template -> SlotBookingTime.builder()
                                    .startTime(template.getStartTime())
                                    .endTime(template.getEndTime())
                                    .build())
                            .collect(Collectors.toList());

                    // 获取该场地的订单项级额外费用
                    List<DetailedPricingInfo.ItemLevelExtraInfo> itemLevelExtras =
                            detailedPricingInfo.getItemLevelExtrasByCourtId()
                                    .getOrDefault(courtId, Collections.emptyList());

                    // 转换为ExtraChargeVo格式
                    int slotCount = courtTemplates.size();
                    Map<Long, Integer> selectedCountByExtraId =
                            selectedItemExtraCountByCourt.getOrDefault(courtId, Collections.emptyMap());
                    List<ExtraChargeVo> extraCharges = itemLevelExtras.stream()
                            .map(extra -> {
                                int selectedCount = isDefaultExtra(extra.getIsDefault())
                                        ? slotCount
                                        : selectedCountByExtraId.getOrDefault(extra.getChargeTypeId(), 0);
                                
                                BigDecimal chargeAmount;
                                if (ChargeModeEnum.PERCENTAGE.getCode().equals(extra.getChargeMode())) {
                                    // 百分比费用：需要根据每个槽位的价格计算
                                    // 遍历选中的槽位，用每个槽位价格 × 百分比
                                    BigDecimal percentageRate = extra.getFixedValue(); // fixedValue存的是百分比值
                                    chargeAmount = BigDecimal.ZERO;
                                    for (VenueBookingSlotTemplate template : courtTemplates) {
                                        // 判断该槽位是否选中了这个费用
                                        boolean isSelected = isDefaultExtra(extra.getIsDefault()) ||
                                                isSlotExtraSelected(template.getBookingSlotTemplateId(), 
                                                        extra.getChargeTypeId(), selectedItemExtraBySlotId);
                                        if (isSelected) {
                                            BigDecimal slotPrice = courtPrices.getOrDefault(template.getStartTime(), BigDecimal.ZERO);
                                            BigDecimal slotCharge = slotPrice.multiply(percentageRate)
                                                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                                            chargeAmount = chargeAmount.add(slotCharge);
                                        }
                                    }
                                } else {
                                    // 固定金额费用：fixedValue × 选中槽位数
                                    BigDecimal fixedAmount = extra.getFixedValue() != null ? extra.getFixedValue() : BigDecimal.ZERO;
                                    chargeAmount = fixedAmount.multiply(new BigDecimal(selectedCount));
                                }
                                
                                return ExtraChargeVo.builder()
                                        .chargeTypeId(extra.getChargeTypeId())
                                        .chargeName(extra.getChargeName())
                                        .chargeMode(ChargeModeEnum.getByCode(extra.getChargeMode()))
                                        .fixedValue(extra.getFixedValue())
                                        .chargeAmount(chargeAmount)
                                        .isDefault(extra.getIsDefault())
                                        .build();
                            })
                            .collect(Collectors.toList());

                    // 计算该item的实际金额（基础金额 + 订单项级附加费用）
                    BigDecimal itemLevelExtraAmount = extraCharges.stream()
                            .map(ExtraChargeVo::getChargeAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal itemAmount = itemBaseAmount.add(itemLevelExtraAmount);

                    // 构建BookingItemVo
                    return BookingItemVo.builder()
                            .courtSnapshot(courtSnapshot)
                            .itemBaseAmount(itemBaseAmount)
                            .itemAmount(itemAmount)
                            .extraCharges(extraCharges)
                            .slotBookingTimes(slotBookingTimes)
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 转换订单级额外费用为ExtraChargeVo
        List<ExtraChargeVo> orderLevelExtraCharges = detailedPricingInfo.getOrderLevelExtras().stream()
                .map(extra -> {
                    boolean selected = isOrderExtraSelected(extra, selectedOrderExtraIdSet);
                    BigDecimal chargeAmount = selected ? extra.getAmount() : BigDecimal.ZERO;
                    return ExtraChargeVo.builder()
                            .chargeTypeId(extra.getChargeTypeId())
                            .chargeName(extra.getChargeName())
                            .chargeMode(ChargeModeEnum.getByCode(extra.getChargeMode()))
                            .chargeAmount(chargeAmount)
                            .fixedValue(extra.getFixedValue())
                            .isDefault(extra.getIsDefault())
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal orderLevelExtraAmount = orderLevelExtraCharges.stream()
                .map(ExtraChargeVo::getChargeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        VenueSnapshotVo venueSnapshotVo = getVenueSnapshotVo(dto.getLatitude(), dto.getLongitude(), venue);
        BigDecimal itemsTotalAmount = items.stream()
                .map(BookingItemVo::getItemAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = itemsTotalAmount.add(orderLevelExtraAmount);

        // 构建返回结果
        return BookingPreviewResponseVo.builder()
                .venueSnapshot(venueSnapshotVo)
                .amount(totalAmount)
                .bookingDate(dto.getBookingDate())
                .isActivity(false)
                .activityTypeName(null)
                .orderLevelExtraCharges(orderLevelExtraCharges)
                .items(items)
                .build();
    }

    /**
     * 活动预览 - 获取活动信息和价格但不锁定活动
     *
     * @param userId 用户ID
     * @param dto 活动预览请求参数
     * @return 包含场馆信息和活动价格信息
     */
    @Override
    public BookingPreviewResponseVo previewActivity(Long userId, ActivityBookingPreviewRequestDto dto) {
        log.info("活动预览 - userId: {}, activityId: {}", userId, dto.getActivityId());

        // 验证并准备活动上下文
        ActivityPreviewContext context = validateAndPrepareActivityContext(dto.getActivityId());
        VenueActivity activity = context.getActivity();
        Venue venue = context.getVenue();
        Court court = context.getCourt();
        ActivityType activityType = context.getActivityType();

        // 构建活动项信息
        CourtSnapshotVo courtSnapshot = CourtSnapshotVo.builder()
                .id(court.getCourtId())
                .name(court.getName())
                .groundType(court.getGroundType())
                .courtType(court.getCourtType())
                .build();

        BigDecimal activityPrice = activity.getUnitPrice() != null ? activity.getUnitPrice() : BigDecimal.ZERO;

        List<SlotBookingTime> slotBookingTimes = Collections.singletonList(
                SlotBookingTime.builder()
                        .startTime(activity.getStartTime())
                        .endTime(activity.getEndTime())
                        .build()
        );

        BookingItemVo item = BookingItemVo.builder()
                .courtSnapshot(courtSnapshot)
                .itemBaseAmount(activityPrice)
                .itemAmount(activityPrice)
                .extraCharges(Collections.emptyList())
                .slotBookingTimes(slotBookingTimes)
                .build();

        // 获取场馆快照
        VenueSnapshotVo venueSnapshotVo = getVenueSnapshotVo(dto.getLatitude(), dto.getLongitude(), venue);

        // 构建返回结果
        return BookingPreviewResponseVo.builder()
                .venueSnapshot(venueSnapshotVo)
                .amount(activityPrice)
                .bookingDate(activity.getActivityDate())
                .isActivity(true)
                .activityTypeName(activityType != null ? activityType.getTypeCode() : null)
                .orderLevelExtraCharges(Collections.emptyList())
                .items(Collections.singletonList(item))
                .currentParticipants(activity.getCurrentParticipants())
                .maxParticipants(activity.getMaxParticipants())
                .build();
    }

    /**
     * 验证并准备槽位预订上下文信息
     */
    @Override
    public SlotBookingContext validateAndPrepareBookingContext(
            List<Long> slotIds, LocalDate bookingDate, Long userId) {

        // 查询槽位模板
        List<VenueBookingSlotTemplate> templates = slotTemplateMapper.selectBatchIds(slotIds);
        if (templates.size() != slotIds.size()) {
            log.warn("部分槽位模板不存在 - 请求{}个，找到{}个", slotIds.size(), templates.size());
            throw new GloboxApplicationException(VenueCode.SLOT_TEMPLATE_NOT_EXIST);
        }

        // 验证槽位是否可用
        validateSlotsAvailability(slotIds, bookingDate);
        // 查询场地信息
        List<Long> courtIds = templates.stream()
                .map(VenueBookingSlotTemplate::getCourtId)
                .distinct()
                .collect(Collectors.toList());
        List<Court> courts = courtMapper.selectBatchIds(courtIds);
        if (courts.isEmpty()) {
            log.warn("场地不存在: courtIds={}", courtIds);
            throw new GloboxApplicationException(VenueCode.COURT_NOT_EXIST);
        }
        //  验证所有槽位来自同一场馆
        Long venueId = courts.get(0).getVenueId();
        boolean allSameVenue = courts.stream()
                .allMatch(court -> court.getVenueId().equals(venueId));
        if (!allSameVenue) {
            log.warn("所选槽位来自不同场馆，不允许 - userId: {}", userId);
            throw new GloboxApplicationException(VenueCode.SLOT_DIFFERENT_VENUE);
        }
        // 查询场馆信息
        Venue venue = venueMapper.selectById(venueId);
        if (venue == null) {
            log.warn("场馆不存在: venueId={}", venueId);
            throw new GloboxApplicationException(VenueCode.VENUE_NOT_EXIST);
        }
        // 构建场地名称映射和场地对象映射
        Map<Long, String> courtNameMap = courts.stream()
                .collect(Collectors.toMap(Court::getCourtId, Court::getName));

        Map<Long, Court> courtMap = courts.stream()
                .collect(Collectors.toMap(Court::getCourtId, court -> court));
        // 构建并返回上下文
        return SlotBookingContext.builder()
                .templates(templates)
                .courts(courts)
                .venue(venue)
                .courtNameMap(courtNameMap)
                .courtMap(courtMap)
                .build();
    }

    /**
     * 验证并准备活动预览上下文信息
     * 公共方法，供RPC服务和预览服务复用
     *
     * @param activityId 活动ID
     * @return 活动预览上下文信息
     */
    @Override
    public ActivityPreviewContext validateAndPrepareActivityContext(Long activityId) {
        // 查询活动详情
        VenueActivity activity = venueActivityMapper.selectById(activityId);
        if (activity == null || VenueActivityStatusEnum.CANCELLED.getValue().equals(activity.getStatus())) {
            log.warn("活动不存在 - activityId={}", activityId);
            throw new GloboxApplicationException(VenueCode.ACTIVITY_NOT_EXIST);
        }

        // 验证活动时间有效性
        LocalDateTime now = LocalDateTime.now();

        // 如果有报名截止时间，检查是否已过期
        if (activity.getRegistrationDeadline() != null && now.isAfter(activity.getRegistrationDeadline())) {
            log.warn("活动报名已截止 - activityId={}, deadline={}", activityId, activity.getRegistrationDeadline());
            throw new GloboxApplicationException(VenueCode.ACTIVITY_REGISTRATION_CLOSED);
        }

        // 检查活动是否已经开始（不允许在活动开始后报名）
        if (activity.getActivityDate() != null && activity.getStartTime() != null) {
            LocalDateTime activityStartTime = LocalDateTime.of(activity.getActivityDate(), activity.getStartTime());
            if (now.isAfter(activityStartTime)) {
                log.warn("活动已开始，无法报名 - activityId={}, activityStartTime={}", activityId, activityStartTime);
                throw new GloboxApplicationException(VenueCode.ACTIVITY_ALREADY_STARTED);
            }
        }

        // 查询场馆信息
        Venue venue = venueMapper.selectById(activity.getVenueId());
        if (venue == null) {
            log.warn("场馆不存在 - venueId={}", activity.getVenueId());
            throw new GloboxApplicationException(VenueCode.VENUE_NOT_EXIST);
        }

        // 查询场地信息
        Court court = courtMapper.selectById(activity.getCourtId());
        if (court == null) {
            log.warn("场地不存在 - courtId={}", activity.getCourtId());
            throw new GloboxApplicationException(VenueCode.COURT_NOT_EXIST);
        }

        // 查询活动类型信息
        ActivityType activityType = null;
        if (activity.getActivityTypeId() != null) {
            activityType = activityTypeMapper.selectById(activity.getActivityTypeId());
        }

        // 构建并返回上下文
        return ActivityPreviewContext.builder()
                .activity(activity)
                .venue(venue)
                .court(court)
                .activityType(activityType)
                .build();
    }

    /**
     * 获取场馆快照信息（公共方法）
     * 供RPC服务和内部服务复用
     * @param userLatitude 用户纬度
     * @param userLongitude 用户经度
     * @return 场馆快照信息
     */
    @Override
    public VenueSnapshotVo getVenueSnapshotVo( Double userLatitude, Double userLongitude,Venue venue) {
        List<String> facilities = venueFacilityRelationMapper.selectList(
                        new LambdaQueryWrapper<VenueFacilityRelation>()
                                .eq(VenueFacilityRelation::getVenueId, venue.getVenueId())
                ).stream()
                .map(VenueFacilityRelation::getFacilityName)
                .toList();
        // 构建场馆快照信息（包含距离计算）
        return VenueSnapshotVo.buildVenueSnapshotVo(
                userLatitude,
                userLongitude,
                venue,
                facilities,
                defaultVenueCoverImage
        );
    }



    /**
     * 执行事务性的预订和计价逻辑,此方法会开启事务
     *
     * @param dto 价格请求DTO
     * @param userName 用户昵称
     * @param context 上下文
     * @return 价格结果DTO
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public PricingResultDto executeBookingInTransaction(
            PricingRequestDto dto,
            SlotBookingContext context,
            String userName) {
        // 二次验证- 确保槽位仍然可用
        validateSlotsAvailability(dto.getSlotIds(),dto.getBookingDate());

        Venue venue = context.getVenue();
        List<VenueBookingSlotTemplate> templates = context.getTemplates();
        Map<Long, String> courtNameMap = context.getCourtNameMap();
        List<Long> selectedOrderExtraIds = normalizeSelectedOrderExtraIds(dto.getSelectedOrderExtraIds());
        Map<Long, List<Long>> selectedItemExtraBySlotId =
                normalizeSelectedItemExtraBySlotId(dto.getSelectedItemExtraBySlotId());
        Set<Long> selectedOrderExtraIdSet = new HashSet<>(selectedOrderExtraIds);

        // 检查Away球场权限
        if(VenueTypeEnum.AWAY.getCode().equals(venue.getVenueType())) {
            log.info("away当前配置{}",awayConfig);
            if((!awayConfig.getBooking().getOpen() || !awayConfig.getBooking().getWhiteListIds().contains(dto.getUserId())) && !awayConfig.getBooking().isAll()) {
                log.error("away订场开关未打开或当前用户不在白名单{}",dto.getUserId());
                throw  new GloboxApplicationException(VenueCode.VENUE_CAN_NOT_BOOKING);
            }
        }

        //先占用本地槽位（不设置thirdPartyBookingId）
        List<VenueBookingSlotRecord> records = lockBookingSlots(
                templates, dto.getUserId(), dto.getBookingDate(), userName, dto.getUserPhone(),context.getVenue().getVenueId());
        if (records == null) {
            log.warn("占用槽位失败 - 槽位已被其他用户占用 userId: {}", dto.getUserId());
            throw new GloboxApplicationException(VenueCode.SLOT_OCCUPIED);
        }
        log.info("本地槽位占用成功，recordIds: {}",
                records.stream()
                        .map(VenueBookingSlotRecord::getBookingSlotRecordId)
                        .toList());
        DetailedPricingInfo detailedPricingInfo;
        // 如果是Away球场，调用第三方平台锁场并更新thirdPartyBookingId，获取Away价格
        List<AwaySlotPrice> awaySlotPrices;
        if(VenueTypeEnum.AWAY.getCode().equals(venue.getVenueType())) {
            LockSlotsResult lockResult = lockInAway(context, dto.getBookingDate(),records);
            awaySlotPrices = lockResult.getSlotPrices();
            detailedPricingInfo = venuePriceServiceImpl.convertAwaySlotPricesToDetailedPricingInfo(awaySlotPrices, templates, context.getCourtMap());
            // 如果是测试情况下,价格设置0.01
            log.info("是否使用测试价格{}",awayConfig.getBooking().isUseTestAmount());
            if(awayConfig.getBooking().isUseTestAmount()) {
                log.info("测试情况下使用测试金额{},原价格{}",awayConfig.getBooking().getTestAmount(),
                        detailedPricingInfo);
                applyAwayTestPricing(detailedPricingInfo,awayConfig.getBooking().getTestAmount());
            }
        } else {
            detailedPricingInfo = venuePriceServiceImpl.calculatePricingByCourtTemplates(
                    templates,
                    venue.getVenueId(),
                    dto.getBookingDate(),
                    context.getCourtMap()
            );
            // 锁场时验证价格：检查每个槽位是否都有价格
            List<String> missingPriceSlots = new ArrayList<>();
            for (VenueBookingSlotTemplate template : templates) {
                Map<LocalTime, BigDecimal> courtPrices = detailedPricingInfo.getPricesByCourtId()
                        .get(template.getCourtId());
                if (courtPrices == null || !courtPrices.containsKey(template.getStartTime())) {
                    missingPriceSlots.add("courtId: " + template.getCourtId() + ", startTime: " + template.getStartTime());
                }
            }
            if (!missingPriceSlots.isEmpty()) {
                log.error("[锁场失败] {}个槽位缺少价格 - {}", missingPriceSlots.size(), missingPriceSlots);
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }
        }
        // 构建templateId -> record的映射
        Map<Long, VenueBookingSlotRecord> templateIdToRecordMap = records.stream()
                .collect(Collectors.toMap(VenueBookingSlotRecord::getSlotTemplateId, record -> record));
        // 从DetailedPricingInfo构建RecordQuote列表
        List<RecordQuote> recordQuotes = templates.stream()
                .map(template -> {
                    Long courtId = template.getCourtId();
                    VenueBookingSlotRecord record = templateIdToRecordMap.get(template.getBookingSlotTemplateId());

                    // 获取该场地的订单项级额外费用
                    List<DetailedPricingInfo.ItemLevelExtraInfo> itemLevelExtras =
                            detailedPricingInfo.getItemLevelExtrasByCourtId().getOrDefault(courtId, Collections.emptyList());

                    // 获取该时段的单价
                    Map<LocalTime, BigDecimal> courtPrices = detailedPricingInfo.getPricesByCourtId()
                            .getOrDefault(courtId, new HashMap<>());
                    BigDecimal unitPrice = courtPrices.getOrDefault(template.getStartTime(), BigDecimal.ZERO);
                    
                    // 转换为ExtraQuote格式
                    List<ExtraQuote> recordExtras = itemLevelExtras.stream()
                            .filter(extra -> isItemExtraSelectedForSlot(
                                    extra,
                                    template.getBookingSlotTemplateId(),
                                    selectedItemExtraBySlotId))
                            .map(extra -> {
                                BigDecimal amount;
                                if (ChargeModeEnum.PERCENTAGE.getCode().equals(extra.getChargeMode())) {
                                    // 百分比费用：使用该槽位价格 × 百分比
                                    amount = unitPrice.multiply(extra.getFixedValue())
                                            .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                                } else {
                                    // 固定金额费用：直接使用fixedValue
                                    amount = extra.getFixedValue();
                                }
                                return ExtraQuote.builder()
                                        .chargeTypeId(extra.getChargeTypeId())
                                        .chargeName(extra.getChargeName())
                                        .chargeMode(ChargeModeEnum.getByCode(extra.getChargeMode()))
                                        .fixedValue(extra.getFixedValue())
                                        .amount(amount)
                                        .build();
                            })
                            .collect(Collectors.toList());

                    return RecordQuote.builder()
                            .recordId(record.getBookingSlotRecordId())  // 使用record的ID
                            .courtId(courtId)
                            .courtName(String.format("%s %s-%s",
                                    courtNameMap.getOrDefault(courtId, "未知场地"),
                                    template.getStartTime(),
                                    template.getEndTime()))
                            .bookingDate(dto.getBookingDate())
                            .startTime(template.getStartTime())
                            .endTime(template.getEndTime())
                            .unitPrice(unitPrice)
                            .recordExtras(recordExtras)
                            .build();
                })
                .collect(Collectors.toList());

        // 转换订单级额外费用格式
        List<OrderLevelExtraQuote> orderLevelExtraQuotes = detailedPricingInfo.getOrderLevelExtras().stream()
                .filter(extra -> isOrderExtraSelected(extra, selectedOrderExtraIdSet))
                .map(extra -> OrderLevelExtraQuote.builder()
                        .chargeTypeId(extra.getChargeTypeId())
                        .chargeName(extra.getChargeName())
                        .chargeMode(ChargeModeEnum.getByCode(extra.getChargeMode()))
                        .amount(extra.getAmount())
                        .build())
                .collect(Collectors.toList());

        // 构建返回结果
        return PricingResultDto.builder()
                .recordQuote(recordQuotes)
                .orderLevelExtras(orderLevelExtraQuotes)
                .sourcePlatform(venue.getVenueType())
                .sellerName(venue.getName())
                .sellerId(venue.getVenueId())
                .bookingDate(dto.getBookingDate())
                .build();
    }

    private List<Long> normalizeSelectedOrderExtraIds(List<Long> selectedOrderExtraIds) {
        return selectedOrderExtraIds == null ? Collections.emptyList() : selectedOrderExtraIds;
    }

    private Map<Long, List<Long>> normalizeSelectedItemExtraBySlotId(Map<Long, List<Long>> selectedItemExtraBySlotId) {
        if (selectedItemExtraBySlotId == null || selectedItemExtraBySlotId.isEmpty()) {
            return Collections.emptyMap();
        }
        return selectedItemExtraBySlotId.entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() == null ? Collections.emptyList() : entry.getValue()
                ));
    }

    private Map<Long, Map<Long, Integer>> buildSelectedItemExtraCountByCourt(
            List<VenueBookingSlotTemplate> templates,
            Map<Long, List<Long>> selectedItemExtraBySlotId) {
        if (templates == null || templates.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Map<Long, Integer>> result = new HashMap<>();
        for (VenueBookingSlotTemplate template : templates) {
            Long templateId = template.getBookingSlotTemplateId();
            List<Long> selectedExtras = selectedItemExtraBySlotId.getOrDefault(templateId, Collections.emptyList());
            if (selectedExtras.isEmpty()) {
                continue;
            }
            Map<Long, Integer> extraCountMap = result.computeIfAbsent(
                    template.getCourtId(), key -> new HashMap<>());
            for (Long extraId : selectedExtras) {
                if (extraId == null) {
                    continue;
                }
                extraCountMap.merge(extraId, 1, Integer::sum);
            }
        }
        return result;
    }

    private boolean isDefaultExtra(Integer isDefault) {
        return isDefault != null && isDefault == 1;
    }

    private boolean isOrderExtraSelected(
            DetailedPricingInfo.OrderLevelExtraInfo extra,
            Set<Long> selectedOrderExtraIdSet) {
        if (extra == null) {
            return false;
        }
        return isDefaultExtra(extra.getIsDefault())
                || (selectedOrderExtraIdSet != null && selectedOrderExtraIdSet.contains(extra.getChargeTypeId()));
    }

    private boolean isItemExtraSelectedForSlot(
            DetailedPricingInfo.ItemLevelExtraInfo extra,
            Long slotTemplateId,
            Map<Long, List<Long>> selectedItemExtraBySlotId) {
        if (extra == null) {
            return false;
        }
        if (isDefaultExtra(extra.getIsDefault())) {
            return true;
        }
        if (slotTemplateId == null) {
            return false;
        }
        List<Long> selectedExtras = selectedItemExtraBySlotId.getOrDefault(slotTemplateId, Collections.emptyList());
        return selectedExtras.contains(extra.getChargeTypeId());
    }
    
    private boolean isSlotExtraSelected(
            Long slotTemplateId,
            Long chargeTypeId,
            Map<Long, List<Long>> selectedItemExtraBySlotId) {
        if (slotTemplateId == null || chargeTypeId == null) {
            return false;
        }
        List<Long> selectedExtras = selectedItemExtraBySlotId.getOrDefault(slotTemplateId, Collections.emptyList());
        return selectedExtras.contains(chargeTypeId);
    }

    /**
     * 占用预订槽位
     * 将槽位状态改为LOCKED_IN，并记录操作人信息和用户信息
     *
     * @param templates 槽位模板列表
     * @param userId 用户ID
     * @param bookingDate 预订日期
     * @param userName 用户昵称
     * @param userPhone 用户手机号
     * @return 返回占用的槽位记录列表（按templates顺序）
     */
    private List<VenueBookingSlotRecord> lockBookingSlots(List<VenueBookingSlotTemplate> templates, Long userId, LocalDate bookingDate, String userName, String userPhone,Long venueId) {
        log.info("开始占用槽位 - userId: {}, 槽位数: {}, userName: {}", userId, templates.size(), userName);

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

        // 分组处理 - 使用 partition 区分要新增和要更新的记录
        Map<Boolean, List<VenueBookingSlotTemplate>> partitioned = templates.stream()
                .collect(Collectors.partitioningBy(
                        t -> existingTemplateIds.contains(t.getBookingSlotTemplateId())));

        // 处理要新增的记录
        List<VenueBookingSlotRecord> toInsert = partitioned.get(false).stream()
                .map(template -> {
                    Long templateId = template.getBookingSlotTemplateId();

                    VenueBookingSlotRecord record = VenueBookingSlotRecord.builder()
                            .slotTemplateId(templateId)
                            .bookingDate(bookingDate.atStartOfDay())
                            .status(BookingSlotStatus.LOCKED_IN.getValue())
                            .lockedType(OperatorSourceEnum.USER.getCode())
                            .operatorId(userId)
                            .operatorSource(OperatorSourceEnum.USER)
                            .userName(userName)
                            .userPhone(userPhone)
                            .venueId(venueId)
                            .build();
                    templateToRecordMap.put(templateId, record);
                    return record;
                })
                .collect(Collectors.toList());

        // 处理要更新的记录
        List<VenueBookingSlotRecord> toUpdate = partitioned.get(true).stream()
                .map(template -> {
                    Long templateId = template.getBookingSlotTemplateId();

                    return existingRecords.stream()
                            .filter(r -> r.getSlotTemplateId().equals(templateId))
                            .peek(record -> {
                                record.setStatus(BookingSlotStatus.LOCKED_IN.getValue());
                                record.setLockedType(OperatorSourceEnum.USER.getCode());
                                record.setOperatorId(userId);
                                record.setOperatorSource(OperatorSourceEnum.USER);
                                record.setUserName(userName);
                                record.setUserPhone(userPhone);
                                templateToRecordMap.put(templateId, record);
                            })
                            .findFirst()
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .toList();

        // 批量insert
        if (!toInsert.isEmpty()) {
            slotRecordService.saveBatch(toInsert);
        }

        // 批量update - 使用原子性更新，只有当前状态为AVAILABLE时才能更新
        // 如果返回0表示槽位已被其他用户占用（超卖防护）
        List<Map.Entry<VenueBookingSlotRecord, Integer>> updateResults = toUpdate.stream()
                .map(record -> new java.util.AbstractMap.SimpleEntry<>(record,
                        slotRecordMapper.updateStatusIfAvailable(
                                record.getBookingSlotRecordId(),
                                BookingSlotStatus.LOCKED_IN.getValue(),
                                userId)))
                .collect(Collectors.toList());

        // 检查是否有失败的更新
        var failedUpdate = updateResults.stream()
                .filter(entry -> entry.getValue() == 0)
                .findFirst();

        if (failedUpdate.isPresent()) {
            VenueBookingSlotRecord record = failedUpdate.get().getKey();
            log.error("【事务内】[lockLock不应该出现这样的错误]槽位已被其他用户占用 - recordId: {}, slotTemplateId: {}",
                    record.getBookingSlotRecordId(), record.getSlotTemplateId());
            return null;  // 返回null表示占用失败
        }

        int successUpdateCount = (int) updateResults.stream().filter(entry -> entry.getValue() > 0).count();

        // 按照 templates 的顺序返回 records，确保顺序正确
        List<VenueBookingSlotRecord> records = templates.stream()
                .map(template -> templateToRecordMap.get(template.getBookingSlotTemplateId()))
                .collect(Collectors.toList());

        log.info("【事务内】槽位占用完成 - userId: {}, 槽位数: {}, 新增: {}, 更新: {} (成功: {}), recordIds: {}",
                userId, templates.size(), toInsert.size(), toUpdate.size(), successUpdateCount,
                records.stream().map(VenueBookingSlotRecord::getBookingSlotRecordId).toArray());

        return records;
    }

    /**
     * 验证槽位是否可用（只读验证）
     * 公共方法，供RPC服务和内部服务复用
     *
     * @param slotTemplateIds 槽位模板ID列表
     * @param bookingDate 预订日期
     */
    private void validateSlotsAvailability(List<Long> slotTemplateIds, LocalDate bookingDate) {
        if (slotTemplateIds == null || slotTemplateIds.isEmpty()) {
            return;
        }

        // 查询是否存在不可用的槽位
        LambdaQueryWrapper<VenueBookingSlotRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(VenueBookingSlotRecord::getSlotTemplateId, slotTemplateIds)
                .eq(VenueBookingSlotRecord::getBookingDate, bookingDate.atStartOfDay())
                .ne(VenueBookingSlotRecord::getStatus, BookingSlotStatus.AVAILABLE.getValue());

        Long count = slotRecordMapper.selectCount(queryWrapper);

        if (count > 0) {
            log.warn("检测到{}个不可用的槽位", count);
            throw new GloboxApplicationException(VenueCode.SLOT_NOT_AVAILABLE);
        }

        log.debug("槽位验证通过 - 所有{}个槽位可用", slotTemplateIds.size());
    }

    /**
     * 获取Away球场的槽位信息（从第三方平台查询）
     *
     * @param venueId 场馆ID
     * @param bookingDate 预订日期
     * @return 场地槽位列表
     */
    private List<CourtSlotVo> getAwayCourtSlots(Long venueId, LocalDate bookingDate) {
        //  查询第三方平台配置
        VenueThirdPartyConfig config = venueThirdPartyConfigMapper.selectOne(
                new LambdaQueryWrapper<VenueThirdPartyConfig>()
                        .eq(VenueThirdPartyConfig::getVenueId, venueId)
        );

        if (config == null) {
            log.warn("Away球场未配置第三方平台 - venueId: {}", venueId);
            return Collections.emptyList();
        }

        //根据 platformId 查询平台信息，获取 platformCode
        ThirdPartyPlatform platform = thirdPartyPlatformMapper.selectById(config.getThirdPartyPlatformId());
        if (platform == null) {
            log.error("第三方平台不存在 - platformId: {}", config.getThirdPartyPlatformId());
            return Collections.emptyList();
        }

        String platformCode = platform.getPlatformCode();
        log.info("获取Away球场槽位 - venueId: {}, platformCode: {}", venueId, platformCode);

        // 根据 platformCode 获取对应的适配器
        ThirdPartyPlatformAdapter adapter = adapterFactory.getAdapter(platformCode);

        // 调用第三方平台查询槽位
        List<ThirdPartyCourtSlotDto> thirdPartySlots = adapter.querySlots(config, bookingDate);

        if (thirdPartySlots == null || thirdPartySlots.isEmpty()) {
            log.info("第三方平台无可用槽位 - venueId: {}, date: {}", venueId, bookingDate);
            return Collections.emptyList();
        }

        // 查询本地场地信息（用于获取场地基本信息）
        // 以Away平台数据为准，本地可能没有对应的场地记录
        List<Court> courts = courtMapper.selectList(
                new LambdaQueryWrapper<Court>()
                        .eq(Court::getVenueId, venueId)
                        .eq(Court::getStatus, CourtStatus.OPEN.getValue())
        );

        // 构建场地ID映射表（thirdPartyCourtId -> Court）
        // 只包含配置了thirdPartyCourtId的本地场地
        Map<String, Court> courtMap = courts.stream()
                .filter(court -> court.getThirdPartyCourtId() != null)
                .collect(Collectors.toMap(Court::getThirdPartyCourtId, court -> court));

        // 批量查询本地槽位模版
        List<Long> courtIds = courts.stream()
                .map(Court::getCourtId)
                .collect(Collectors.toList());

        List<VenueBookingSlotTemplate> templates = slotTemplateMapper.selectList(
                new LambdaQueryWrapper<VenueBookingSlotTemplate>()
                        .in(VenueBookingSlotTemplate::getCourtId, courtIds)
        );

        //  构建槽位模版映射表（courtId + startTime + endTime -> template）
        Map<String, VenueBookingSlotTemplate> templateMap = templates.stream()
                .collect(Collectors.toMap(
                        template -> buildTemplateKey(template.getCourtId(), template.getStartTime(), template.getEndTime()),
                        template -> template
                ));

        // 同步Away数据：插入缺失的槽位，移除多余的槽位（批量操作）
        syncAwaySlots(venueId, bookingDate, thirdPartySlots, courtMap, templateMap);

        //  转换第三方数据为CourtSlotVo格式
        List<String> notFoundCourts = new ArrayList<>();
        List<CourtSlotVo> result = thirdPartySlots.stream()
                .map(slot -> {
                    CourtSlotVo vo = convertThirdPartyDtoToCourtSlotVo(slot, courtMap, templateMap);
                    if (vo == null) {
                        notFoundCourts.add("thirdPartyCourtId: " + slot.getThirdPartyCourtId() +
                                ", courtName: " + slot.getCourtName());
                    }
                    return vo;
                })
                .filter(Objects::nonNull)  // 过滤掉无法匹配的场地
                .collect(Collectors.toList());

        if (!notFoundCourts.isEmpty()) {
            log.error("本地未找到对应场地，跳过{}个场地 - venueId: {}, 场地列表: {}",
                    notFoundCourts.size(), venueId, notFoundCourts);
        }
        log.info("获取Away球场槽位成功 - venueId: {}, 场地数: {}", venueId, result.size());
        return result;
    }

    /**
     * 将统一的ThirdPartyCourtSlotDto转换为CourtSlotVo格式
     *
     * @param dto 统一的第三方槽位DTO
     * @param courtMap 场地映射表
     * @param templateMap 槽位模版映射表
     * @return CourtSlotVo
     */
    private CourtSlotVo convertThirdPartyDtoToCourtSlotVo(ThirdPartyCourtSlotDto dto, Map<String, Court> courtMap, Map<String, VenueBookingSlotTemplate> templateMap) {
        // 根据第三方场地ID查找本地场地信息
        Court court = courtMap.get(dto.getThirdPartyCourtId());
        if (court == null) {
            // 本地没有对应场地，直接不展示（错误日志在外层统一打印）
            return null;
        }

        // 转换槽位列表（将第三方的不规则槽位拆分成30分钟的槽位）
        List<ThirdPartySlotDto> slotsList = dto.getSlots();
        List<BookingSlotVo> slots = new ArrayList<>();
        if (slotsList == null || slotsList.isEmpty()) {
            log.warn("第三方场地槽位列表为空 - courtId: {}", court.getCourtId());
            return CourtSlotVo.builder()
                    .courtId(court.getCourtId())
                    .courtName(dto.getCourtName() != null ? dto.getCourtName() : court.getName())
                    .courtType(court.getCourtType())
                    .courtTypeDesc(CourtType.fromValue(court.getCourtType()).getDescription())
                    .groundType(court.getGroundType())
                    .groundTypeDesc(GroundType.fromValue(court.getGroundType()).getDescription())
                    .slots(Collections.emptyList())
                    .build();
        }

        try {
            for (ThirdPartySlotDto slotDto : slotsList) {
                // 处理"24:00"的情况 有些平台可能以24:00为时间结尾,与我们系统设计不符
                String startTimeStr = slotDto.getStartTime();
                String endTimeStr = slotDto.getEndTime();
                if ("24:00".equals(endTimeStr)) {
                    endTimeStr = "23:59";
                }

                LocalTime startTime = LocalTime.parse(startTimeStr);
                LocalTime endTime = LocalTime.parse(endTimeStr);

                // 将第三方槽位拆分成30分钟的槽位
                List<BookingSlotVo> splitSlots = splitSlotToHalfHourSlots(court.getCourtId(), startTime, endTime, slotDto, templateMap);
                slots.addAll(splitSlots);
            }

            // 按开始时间排序
            slots.sort(Comparator.comparing(BookingSlotVo::getStartTime));
        } catch (Exception e) {
            log.error("处理槽位异常 - courtId: {}", court.getCourtId(), e);
        }

        log.info("槽位拆分完成 - courtId: {}, 拆分后槽位数量: {}", court.getCourtId(), slots.size());

        // 构建CourtSlotVo
        return CourtSlotVo.builder()
                .courtId(court.getCourtId())
                .courtName(dto.getCourtName() != null ? dto.getCourtName() : court.getName())
                .courtType(court.getCourtType())
                .courtTypeDesc(CourtType.fromValue(court.getCourtType()).getDescription())
                .groundType(court.getGroundType())
                .groundTypeDesc(GroundType.fromValue(court.getGroundType()).getDescription())
                .slots(slots)
                .build();
    }

    /**
     * 将第三方的不规则槽位拆分成30分钟的槽位
     *
     * @param courtId 场地ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param originalSlot 原始第三方槽位DTO
     * @param templateMap 槽位模板Map
     * @return 拆分后的槽位列表
     */
    private List<BookingSlotVo> splitSlotToHalfHourSlots(Long courtId, LocalTime startTime, LocalTime endTime,
                                                          ThirdPartySlotDto originalSlot,
                                                          Map<String, VenueBookingSlotTemplate> templateMap) {
        List<BookingSlotVo> result = new ArrayList<>();

        // 先获取全部拆分结果，用于判断是否需要linked标记和价格平分
        List<TimeSlotSplitUtil.TimeSlot> splitSlots = TimeSlotSplitUtil.splitTimeSlots(startTime, endTime);
        int splitCount = splitSlots.size();
        boolean isLinked = splitCount > 1;

        // 如果原始槽位>30分钟，按比例平分价格
        BigDecimal pricePerSlot;
        if (isLinked && originalSlot.getPrice() != null) {
            pricePerSlot = originalSlot.getPrice()
                    .divide(BigDecimal.valueOf(splitCount), 2, RoundingMode.HALF_UP);
        } else {
            pricePerSlot = originalSlot.getPrice();
        }

        for (TimeSlotSplitUtil.TimeSlot slot : splitSlots) {
            LocalTime currentTime = slot.getStartTime();
            LocalTime slotEnd = slot.getEndTime();

            // 查找对应的模板
            String templateKey = buildTemplateKey(courtId, currentTime, slotEnd);
            VenueBookingSlotTemplate template = templateMap.get(templateKey);

            if (template != null) {
                BookingSlotVo slotVo = convertThirdPartySlotDtoToBookingSlotVo(
                        originalSlot,
                        template.getBookingSlotTemplateId(),
                        currentTime,
                        slotEnd,
                        pricePerSlot,
                        isLinked
                );
                result.add(slotVo);
            }
        }

        return result;
    }

    /**
     * 将统一的ThirdPartySlotDto转换为BookingSlotVo（带自定义时间和价格）
     *
     * @param slotDto 统一的第三方槽位DTO
     * @param templateId 本地槽位模版ID
     * @param startTime 自定义开始时间
     * @param endTime 自定义结束时间
     * @param splitPrice 拆分后的价格（如1小时120拆成2个30分钟各60）
     * @param linked 是否连续槽位（从更大时间单位拆分而来）
     * @return BookingSlotVo
     */
    private BookingSlotVo convertThirdPartySlotDtoToBookingSlotVo(ThirdPartySlotDto slotDto, Long templateId,
                                                                   LocalTime startTime, LocalTime endTime,
                                                                   BigDecimal splitPrice, boolean linked) {
        // 判断状态
        boolean isAvailable = Boolean.TRUE.equals(slotDto.getAvailable());
        Integer status = isAvailable ? BookingSlotStatus.AVAILABLE.getValue() : BookingSlotStatus.EXPIRED.getValue();
        String statusDesc = isAvailable ? BookingSlotStatus.AVAILABLE.getDescription() : BookingSlotStatus.LOCKED_IN.getDescription();

        return BookingSlotVo.builder()
                .bookingSlotId(templateId)  // 使用本地槽位模版ID
                .startTime(startTime)
                .endTime(endTime)
                .slotType(SlotTypeEnum.NORMAL.getCode())  // 普通槽位
                .status(status)
                .statusDesc(statusDesc)
                .isAvailable(isAvailable)
                .price(splitPrice)
                .isMyBooking(false)  // Away球场默认非本人预订
                .linked(linked)
                .build();
    }

    /**
     * 构建槽位模版的唯一Key
     *
     * @param courtId 场地ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 唯一Key
     */
    private String buildTemplateKey(Long courtId, LocalTime startTime, LocalTime endTime) {
        // 如果endTime是00:00:00（表示午夜/次日0点）或23:59:00，统一转换为23:59:59
        LocalTime normalizedEndTime = (LocalTime.MIDNIGHT.equals(endTime) || LocalTime.of(23, 59).equals(endTime))
                ? LocalTime.of(23, 59, 59) : endTime;
        return courtId + "_" + startTime + "_" + normalizedEndTime;
    }

    /**
     * 同步Away数据到本地：插入缺失的槽位，从templateMap中移除多余的槽位
     * @param venueId 场馆ID
     * @param bookingDate 预订日期
     * @param thirdPartySlots Away平台返回的槽位数据
     * @param courtMap 本地场地映射表（thirdPartyCourtId -> Court）
     * @param templateMap 本地槽位模板映射表，会被修改（添加新槽位，移除多余槽位）
     */
    private void syncAwaySlots(Long venueId, LocalDate bookingDate,
                               List<ThirdPartyCourtSlotDto> thirdPartySlots,
                               Map<String, Court> courtMap,
                               Map<String, VenueBookingSlotTemplate> templateMap) {

        log.info("[Away槽位同步] 开始同步 - venueId: {}, date: {}", venueId, bookingDate);

        //  构建本地槽位键集合（courtId_startTime）
        Set<String> localSlotKeys = templateMap.keySet().stream()
                .map(key -> {
                    // key格式: courtId_startTime_endTime，需要提取前两部分
                    String[] parts = key.split("_");
                    if (parts.length >= 2) {
                        return parts[0] + "_" + parts[1];  // courtId_startTime
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        //构建Away槽位键集合，并找出缺失的槽位（待插入）
        Set<String> awaySlotKeys = new HashSet<>();
        List<VenueBookingSlotTemplate> slotsToInsert = new ArrayList<>();

        thirdPartySlots.forEach(awayCourtSlot -> {
            String thirdPartyCourtId = awayCourtSlot.getThirdPartyCourtId();
            Court court = courtMap.get(thirdPartyCourtId);
            // 只处理本地已配置的场地
            if (court != null && awayCourtSlot.getSlots() != null) {
                Long courtId = court.getCourtId();
                awayCourtSlot.getSlots().forEach(slot -> {
                    // 处理24:00的情况
                    String startTimeStr = slot.getStartTime();
                    String endTimeStr = slot.getEndTime();
                    if ("24:00".equals(endTimeStr)) {
                        endTimeStr = "23:59";
                    }

                    LocalTime startTime = LocalTime.parse(startTimeStr);
                    LocalTime endTime = LocalTime.parse(endTimeStr);

                    // 将槽位拆分成30分钟，并将每个拆分后的槽位加入awaySlotKeys
                    TimeSlotSplitUtil.splitTimeSlots(startTime, endTime, splitSlot -> {
                        LocalTime splitStart = splitSlot.getStartTime();
                        // 如果endTime是00:00:00（表示午夜/次日0点）或23:59:00，统一转换为23:59:59
                        LocalTime rawEnd = splitSlot.getEndTime();
                        LocalTime splitEnd = (LocalTime.MIDNIGHT.equals(rawEnd) || LocalTime.of(23, 59).equals(rawEnd))
                                ? LocalTime.of(23, 59, 59) : rawEnd;
                        String awaySlotKey = courtId + "_" + splitStart.toString();
                        awaySlotKeys.add(awaySlotKey);

                        // 如果本地不存在该槽位，加入待插入列表
                        if (!localSlotKeys.contains(awaySlotKey)) {
                            VenueBookingSlotTemplate template = VenueBookingSlotTemplate.builder()
                                    .courtId(courtId)
                                    .startTime(splitStart)
                                    .endTime(splitEnd)
                                    .build();
                            slotsToInsert.add(template);
                            log.info("[Away槽位同步] 发现缺失槽位 - courtId: {}, startTime: {}, endTime: {}",
                                    courtId, splitStart, splitEnd);
                        }
                    });
                });
            }
        });
        //  批量插入缺失的槽位
        if (!slotsToInsert.isEmpty()) {
            try {
                slotsToInsert.forEach(slotTemplateMapper::insert);
                log.info("[Away槽位同步] 批量插入缺失槽位成功 - 插入数: {}", slotsToInsert.size());
                // 将新插入的槽位加入templateMap
                slotsToInsert.forEach(newSlot -> {
                    String newKey = buildTemplateKey(newSlot.getCourtId(), newSlot.getStartTime(), newSlot.getEndTime());
                    templateMap.put(newKey, newSlot);
                });
            } catch (Exception e) {
                log.error("[Away槽位同步] 批量插入缺失槽位失败", e);
                throw e;
            }
        }

        //  从templateMap中移除本地多余的槽位（本地有但Away没有）
        List<String> keysToRemove = templateMap.keySet().stream()
                .filter(key -> {
                    // key格式: courtId_startTime_endTime，需要提取前两部分
                    String[] parts = key.split("_");
                    if (parts.length >= 2) {
                        String slotKey = parts[0] + "_" + parts[1];  // courtId_startTime
                        return !awaySlotKeys.contains(slotKey);  // 本地有但Away没有
                    }
                    return false;
                })
                .toList();
        if (!keysToRemove.isEmpty()) {
            keysToRemove.forEach(templateMap::remove);
            log.warn("[Away槽位同步] 移除本地多余的槽位 - 移除数量: {}, 槽位列表: {}", keysToRemove.size(), keysToRemove);
        }
        log.info("[Away槽位同步] 完成 - 新增: {}, 移除: {}", slotsToInsert.size(), keysToRemove.size());
    }

    /**
     * 调用第三方平台锁场，获取bookingIds和价格信息
     * @param context 整个下单流程的上下文
     * @param data 预订日期
     * @return LockSlotsResult - 包含bookingIds映射和价格信息
     */
    private LockSlotsResult lockInAway(SlotBookingContext context, LocalDate data,List<VenueBookingSlotRecord> records) {
        Venue venue = context.getVenue();
        List<VenueBookingSlotTemplate> templates = context.getTemplates();
        List<Court> courts = context.getCourts();
        VenueThirdPartyConfig config = venueThirdPartyConfigMapper.selectOne(
                new LambdaQueryWrapper<VenueThirdPartyConfig>()
                        .eq(VenueThirdPartyConfig::getVenueId, venue.getVenueId()));
        if (config == null) {
            log.error("Away球场未配置第三方平台 - venueId: {}", venue.getVenueId());
            throw new GloboxApplicationException(VenueCode.VENUE_CAN_NOT_BOOKING);
        }
        //根据 platformId 查询平台信息，获取 platformCode
        ThirdPartyPlatform platform = thirdPartyPlatformMapper.selectById(config.getThirdPartyPlatformId());
        if (platform == null) {
            log.error("第三方平台不存在 - platformId: {}", config.getThirdPartyPlatformId());
            throw new GloboxApplicationException(VenueCode.VENUE_CAN_NOT_BOOKING);
        }
        String platformCode = platform.getPlatformCode();
        log.info("Away球场锁场 - venueId: {}, platformCode: {}", venue.getVenueId(), platformCode);

        // 根据 platformCode 获取对应的适配器
        ThirdPartyPlatformAdapter adapter = adapterFactory.getAdapter(platformCode);

        // 检查是否所有场地都配置了第三方id
        List<String> missingThirdPartyIdCourts = courts.stream()
                .filter(court -> court.getThirdPartyCourtId() == null)
                .map(court -> "courtId: " + court.getCourtId() + ", courtName: " + court.getName())
                .collect(Collectors.toList());

        if (!missingThirdPartyIdCourts.isEmpty()) {
            log.error("第三方球馆{}:{}中有{}个场地未配置第三方id,预定失败 - 场地列表: {}",
                    venue.getVenueId(), venue.getName(), missingThirdPartyIdCourts.size(), missingThirdPartyIdCourts);
            throw new GloboxApplicationException(VenueCode.VENUE_CAN_NOT_BOOKING);
        }

        Map<Long, String> courtThirdPartyIdMap = courts.stream()
                .collect(Collectors.toMap(
                        Court::getCourtId,
                        Court::getThirdPartyCourtId,
                        (existingValue, newValue) -> existingValue
                ));

        // 生成锁场备注（格式：{LOCK_REMARK_PREFIX}_{时段}_{时间戳}）用于解锁时校验
        long lockTimestamp = System.currentTimeMillis();
        String timeSlotRange = templates.stream()
                .map(t -> t.getStartTime().toString())
                .sorted()
                .collect(Collectors.joining(","));
        String thirdPartyRemark = String.format("%s_%s_%d", AwayVenueCacheConstants.LOCK_REMARK_PREFIX, timeSlotRange, lockTimestamp);
        log.info("[Away锁场] 生成锁场备注: {}", thirdPartyRemark);

        // 构建SlotLockRequest列表
        List<SlotLockRequest> slotLockRequests = templates.stream()
                        .map(template -> {
                            String thirdPartyCourtId = courtThirdPartyIdMap.get(template.getCourtId());
                            String startTime = template.getStartTime().toString();
                            String endTime = template.getEndTime().toString();
                            return SlotLockRequest.builder()
                                    .thirdPartyCourtId(thirdPartyCourtId)
                                    .startTime(startTime)
                                    .endTime(endTime)
                                    .date(data)
                                    .build();
                        }).toList();

        // 调用第三方平台锁场 如果锁场失败,内部会抛出异常
        // 但是如果锁场成功,而拿到的BookingId为空,那视为锁场成功,后续需要人工干预去解锁
        // 出现这种情况(锁场成功后的查询订单id/锁场id 失败),可能是第二次查询超时
        LockSlotsResult lockResult = adapter.lockSlots(config, slotLockRequests, thirdPartyRemark);
        log.info("[Away锁场] 第三方平台锁场成功 - venueId: {},  价格信息: {}",
                venue.getVenueId(),lockResult.getSlotPrices());
        
        // 构建templateId -> thirdPartyBookingId的映射（即使bookingIds为空也要保存remark）
        Map<Long, String> templateIdToThirdPartyBookingIdMap = new HashMap<>();
        
        if(!ObjectUtils.isEmpty(lockResult.getBookingIds())) {
            List<String> unmatchedTemplates = new ArrayList<>();
            templateIdToThirdPartyBookingIdMap = lockResult.getBookingIds().entrySet().stream()
                    .map(entry -> {
                        SlotLockRequest slotRequest = entry.getKey();
                        String thirdPartyBookingId = entry.getValue();

                        // 找到对应的template
                        VenueBookingSlotTemplate matchedTemplate = templates.stream()
                                .filter(template -> {
                                    String thirdPartyCourtId = courtThirdPartyIdMap.get(template.getCourtId());
                                    return thirdPartyCourtId != null
                                            && thirdPartyCourtId.equals(slotRequest.getThirdPartyCourtId())
                                            && template.getStartTime().toString().equals(slotRequest.getStartTime())
                                            && template.getEndTime().toString().equals(slotRequest.getEndTime());
                                })
                                .findFirst()
                                .orElse(null);

                        if (matchedTemplate == null) {
                            unmatchedTemplates.add("courtId: " + slotRequest.getThirdPartyCourtId() +
                                    ", time: " + slotRequest.getStartTime() + "-" + slotRequest.getEndTime());
                            return null;
                        }

                        return new AbstractMap.SimpleEntry<>(matchedTemplate.getBookingSlotTemplateId(), thirdPartyBookingId);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (!unmatchedTemplates.isEmpty()) {
                log.error("[Away锁场] 未找到匹配的template - 数量: {}, 列表: {}",
                        unmatchedTemplates.size(), unmatchedTemplates);
            }
        }
        
        if(templateIdToThirdPartyBookingIdMap.size() < templates.size()) {
            // 获取到的id不全或者获取不到，锁场失败（解锁时需要bookingId和remark都有效）
            log.error("[lockInAway]获取到的bookingIds不全或者获取不到,场馆: {}->{},时间:{},获取到:{}/{}",
                    venue.getVenueId(),venue.getName(),data, templateIdToThirdPartyBookingIdMap.size(), templates.size());
        }
        
        // 保存bookingId和thirdPartyRemark
        try {
            updateRecordsThirdPartyBookingIdAndRemark(records, templateIdToThirdPartyBookingIdMap, thirdPartyRemark);
        }catch (Exception e) {
            log.error("[Away锁场] 成功,但是插入第三方锁场id/remark失败,场馆:{} -> {}",venue.getVenueId(),venue.getName(), e);
            return lockResult;
        }
        log.info("[Away锁场] 已更新{}条records的thirdPartyBookingId和thirdPartyRemark, remark: {}", records.size(), thirdPartyRemark);
        // 返回LockSlotsResult，包含bookingIds映射和价格信息
        return lockResult;
    }

    /**
     * 批量更新records的thirdPartyBookingId和thirdPartyRemark
     *
     * @param records 要更新的records列表
     * @param templateIdToThirdPartyBookingIdMap templateId -> thirdPartyBookingId的映射
     * @param thirdPartyRemark 第三方平台锁场备注（用于解锁时校验）
     */
    private void updateRecordsThirdPartyBookingIdAndRemark(
        List<VenueBookingSlotRecord> records, Map<Long, String> templateIdToThirdPartyBookingIdMap, String thirdPartyRemark) {

        // 为每个record设置thirdPartyBookingId和thirdPartyRemark
        records.forEach(record -> {
            String thirdPartyBookingId = templateIdToThirdPartyBookingIdMap.get(record.getSlotTemplateId());
            if (thirdPartyBookingId != null) {
                record.setThirdPartyBookingId(thirdPartyBookingId);
            }
            record.setThirdPartyRemark(thirdPartyRemark);
        });
        // 批量更新
        slotRecordService.updateBatchById(records);
    }


    /**
     * 测试场景为away设置测试价格
     * @param detailedPricingInfo
     * @param testAmount
     */
    private void applyAwayTestPricing(DetailedPricingInfo detailedPricingInfo, BigDecimal testAmount) {
        if (detailedPricingInfo == null || testAmount == null) {
            return;
        }

        Map<Long, Map<LocalTime, BigDecimal>> pricesByCourtId =
                detailedPricingInfo.getPricesByCourtId();

        if (pricesByCourtId != null) {
            pricesByCourtId.values().stream()
                    .filter(Objects::nonNull)
                    .forEach(pricesByTime ->
                            pricesByTime.replaceAll((k, v) -> testAmount)
                    );
        }

        List<DetailedPricingInfo.OrderLevelExtraInfo> orderLevelExtras =
                detailedPricingInfo.getOrderLevelExtras();

        if (orderLevelExtras != null) {
            orderLevelExtras.stream()
                    .filter(Objects::nonNull)
                    .forEach(extra -> extra.setAmount(BigDecimal.ZERO));
        }

        Map<Long, List<DetailedPricingInfo.ItemLevelExtraInfo>> itemLevelExtrasByCourtId =
                detailedPricingInfo.getItemLevelExtrasByCourtId();

        if (itemLevelExtrasByCourtId != null) {
            itemLevelExtrasByCourtId.values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .filter(Objects::nonNull)
                    .forEach(extra -> {});
        }

        detailedPricingInfo.setOrderLevelExtraAmount(BigDecimal.ZERO);
        detailedPricingInfo.setBasePrice(testAmount);
        detailedPricingInfo.setTotalPrice(testAmount);
    }

}
