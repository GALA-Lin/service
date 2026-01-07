package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.enums.order.ChargeModeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.VenueCode;
import com.unlimited.sports.globox.dubbo.merchant.dto.*;
import com.unlimited.sports.globox.model.venue.dto.*;
import com.unlimited.sports.globox.model.venue.enums.*;
import com.unlimited.sports.globox.model.venue.vo.BookingItemVo;
import com.unlimited.sports.globox.model.venue.vo.CourtSnapshotVo;
import com.unlimited.sports.globox.model.venue.vo.ExtraChargeVo;
import com.unlimited.sports.globox.model.venue.vo.VenueSnapshotVo;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueFacilityRelationMapper;
import com.unlimited.sports.globox.venue.dto.ActivityPreviewContext;
import com.unlimited.sports.globox.venue.dto.PricingInfo;
import com.unlimited.sports.globox.venue.dto.SlotBookingContext;
import com.unlimited.sports.globox.venue.mapper.VenueActivityMapper;
import com.unlimited.sports.globox.venue.mapper.ActivityTypeMapper;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.merchant.entity.VenueBusinessHours;
import com.unlimited.sports.globox.model.merchant.enums.BusinessHourRuleTypeEnum;
import com.unlimited.sports.globox.model.venue.vo.SlotBookingTime;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotRecord;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueFacilityRelation;
import com.unlimited.sports.globox.model.venue.entity.venues.ActivityType;
import com.unlimited.sports.globox.model.venue.vo.CourtSlotVo;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotRecordMapper;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotTemplateMapper;
import com.unlimited.sports.globox.venue.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Value("${default_image.venue_cover}")
    private String defaultVenueCoverImage;


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

        // 批量获取所有槽位的价格
        List<LocalTime> slotStartTimes = allTemplates.stream()
                .map(VenueBookingSlotTemplate::getStartTime)
                .distinct()
                .collect(Collectors.toList());

        if(venue.getTemplateId() == null) {
            throw new GloboxApplicationException(VenueCode.VENUE_PRICE_NOT_CONFIGURED);
        }
        Map<LocalTime, BigDecimal> priceMap = venuePriceService.getSlotPriceMap(
                venue.getTemplateId(),
                venue.getVenueId(),
                bookingDate,
                slotStartTimes
        );

        log.debug("批量获取价格完成，共{}个时间点", priceMap.size());

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


        // 构建场地槽位结果，过滤掉没有槽位模板的场地
        return courts.stream()
                .map(court -> CourtSlotVo.buildVo(
                        court,
                        templatesByCourtId.getOrDefault(court.getCourtId(), Collections.emptyList()),
                        recordMap,
                        priceMap,
                        activityMap,
                        activityLockedSlots,
                        dto.getUserId()
                ))
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

        // 计算完整价格（槽位价格 + 订单级额外费用）
        PricingInfo pricingInfo = venuePriceService.calculateCompletePricingByTemplates(templates,
                venue.getVenueId(),
                venue.getTemplateId(),
                dto.getBookingDate());

        // 按场地分组模板，构建BookingItemVo列表
        Map<Long, List<VenueBookingSlotTemplate>> templatesByCourtId = templates.stream()
                .collect(Collectors.groupingBy(VenueBookingSlotTemplate::getCourtId));

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

                    // 计算该场地的基础金额
                    BigDecimal itemBaseAmount = courtTemplates.stream()
                            .map(template -> pricingInfo.getSlotPrices().getOrDefault(template.getStartTime(), BigDecimal.ZERO))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // 构建时间段列表
                    List<SlotBookingTime> slotBookingTimes = courtTemplates.stream()
                            .map(template -> SlotBookingTime.builder()
                                    .startTime(template.getStartTime())
                                    .endTime(template.getEndTime())
                                    .build())
                            .collect(Collectors.toList());

                    // 获取该场地的订单项级额外费用
                    List<PricingInfo.ItemLevelExtraInfo> itemLevelExtras =
                            pricingInfo.getItemLevelExtrasByCourtId().getOrDefault(courtId, Collections.emptyList());

                    // 转换为ExtraChargeVo格式
                    List<ExtraChargeVo> extraCharges = itemLevelExtras.stream()
                            .map(extra -> ExtraChargeVo.builder()
                                    .chargeTypeId(extra.getChargeTypeId())
                                    .chargeName(extra.getChargeName())
                                    .chargeMode(ChargeModeEnum.getByCode(extra.getChargeMode()))
                                    .fixedValue(extra.getFixedValue())
                                    .chargeAmount(extra.getAmount())
                                    .build())
                            .collect(Collectors.toList());

                    // 计算该item的实际金额（基础金额 + 订单项级附加费用）
                    BigDecimal itemLevelExtraAmount = itemLevelExtras.stream()
                            .map(PricingInfo.ItemLevelExtraInfo::getAmount)
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
        List<ExtraChargeVo> orderLevelExtraCharges = pricingInfo.getOrderLevelExtras().stream()
                .map(extra -> ExtraChargeVo.builder()
                        .chargeTypeId(extra.getChargeTypeId())
                        .chargeName(extra.getChargeName())
                        .chargeMode(ChargeModeEnum.getByCode(extra.getChargeMode()))
                        .chargeAmount(extra.getAmount())
                        .fixedValue(extra.getFixedValue())
                        .build())
                .collect(Collectors.toList());

        VenueSnapshotVo venueSnapshotVo = getVenueSnapshotVo(dto.getLatitude(), dto.getLongitude(), venue);

        // 构建返回结果
        return BookingPreviewResponseVo.builder()
                .venueSnapshot(venueSnapshotVo)
                .amount(pricingInfo.getTotalPrice())
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
        if (activity == null) {
            log.warn("活动不存在 - activityId={}", activityId);
            throw new GloboxApplicationException(VenueCode.ACTIVITY_NOT_EXIST);
        }

        // 验证活动是否有效（报名期限未到期）
        LocalDateTime now = LocalDateTime.now();
        if (activity.getRegistrationDeadline() != null && now.isAfter(activity.getRegistrationDeadline())) {
            log.warn("活动报名已截止 - activityId={}", activityId);
            throw new GloboxApplicationException(VenueCode.ACTIVITY_REGISTRATION_CLOSED);
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
     * @param templates 槽位模板列表
     * @param venue 场馆对象（外部已查询）
     * @return 价格结果DTO
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public PricingResultDto executeBookingInTransaction(
            PricingRequestDto dto,
            List<VenueBookingSlotTemplate> templates,
            Venue venue,
            Map<Long, String> courtNameMap) {
        // 二次验证- 确保槽位仍然可用
        validateSlotsAvailability(dto.getSlotIds(),dto.getBookingDate());
        // 原子性地更新槽位状态，防止超卖
        List<VenueBookingSlotRecord> records = lockBookingSlots(
                templates, dto.getUserId(), dto.getBookingDate());
        if (records == null) {
            log.warn("占用槽位失败 - 槽位已被其他用户占用 userId: {}", dto.getUserId());
            throw new GloboxApplicationException(VenueCode.SLOT_OCCUPIED);
        }
        log.debug("槽位占用成功，recordIds: {}",
                records.stream()
                        .map(VenueBookingSlotRecord::getBookingSlotRecordId)
                        .toList());
        // 计算完整价格
        PricingInfo pricingInfo =
                venuePriceService.calculateCompletePricingByTemplates(
                        templates,
                        venue.getVenueId(),
                        venue.getTemplateId(),
                        dto.getBookingDate()
                );
        log.info("预订逻辑执行成功 - userId: {}, 槽位数: {}", dto.getUserId(), records.size());

        // 从PricingInfo构建RecordQuote列表
        List<RecordQuote> recordQuotes = templates.stream()
                .map(template -> {
                    Long courtId = template.getCourtId();
                    // 获取该场地的订单项级额外费用
                    List<PricingInfo.ItemLevelExtraInfo> itemLevelExtras =
                            pricingInfo.getItemLevelExtrasByCourtId().getOrDefault(courtId, Collections.emptyList());

                    // 转换为ExtraQuote格式
                    List<ExtraQuote> recordExtras = itemLevelExtras.stream()
                            .map(extra -> ExtraQuote.builder()
                                    .chargeTypeId(extra.getChargeTypeId())
                                    .chargeName(extra.getChargeName())
                                    .chargeMode(ChargeModeEnum.getByCode(extra.getChargeMode()))
                                    .fixedValue(extra.getFixedValue())
                                    .amount(extra.getAmount())
                                    .build())
                            .collect(Collectors.toList());

                    return RecordQuote.builder()
                            .recordId(template.getBookingSlotTemplateId())
                            .courtId(courtId)
                            .courtName(String.format("%s %s-%s",
                                    courtNameMap.getOrDefault(courtId, "未知场地"),
                                    template.getStartTime(),
                                    template.getEndTime()))
                            .bookingDate(dto.getBookingDate())
                            .startTime(template.getStartTime())
                            .endTime(template.getEndTime())
                            .unitPrice(pricingInfo.getSlotPrices().getOrDefault(template.getStartTime(), BigDecimal.ZERO))
                            .recordExtras(recordExtras)
                            .build();
                })
                .collect(Collectors.toList());

        // 转换订单级额外费用格式
        List<OrderLevelExtraQuote> orderLevelExtraQuotes = pricingInfo.getOrderLevelExtras().stream()
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

        // 分组处理 - 使用 partition 区分要新增和要更新的记录
        Map<Boolean, List<VenueBookingSlotTemplate>> partitioned = templates.stream()
                .collect(Collectors.partitioningBy(
                        t -> existingTemplateIds.contains(t.getBookingSlotTemplateId())));

        // 处理要新增的记录
        List<VenueBookingSlotRecord> toInsert = partitioned.get(false).stream()
                .map(template -> {
                    VenueBookingSlotRecord record = VenueBookingSlotRecord.builder()
                            .slotTemplateId(template.getBookingSlotTemplateId())
                            .bookingDate(bookingDate.atStartOfDay())
                            .status(BookingSlotStatus.LOCKED_IN.getValue())
                            .operatorId(userId)
                            .operatorSource(OperatorSourceEnum.USER)
                            .build();
                    templateToRecordMap.put(template.getBookingSlotTemplateId(), record);
                    return record;
                })
                .collect(Collectors.toList());

        // 处理要更新的记录
        List<VenueBookingSlotRecord> toUpdate = partitioned.get(true).stream()
                .map(template -> existingRecords.stream()
                        .filter(r -> r.getSlotTemplateId().equals(template.getBookingSlotTemplateId()))
                        .peek(record -> {
                            record.setStatus(BookingSlotStatus.LOCKED_IN.getValue());
                            record.setOperatorId(userId);
                            record.setOperatorSource(OperatorSourceEnum.USER);
                            templateToRecordMap.put(template.getBookingSlotTemplateId(), record);
                        })
                        .findFirst()
                        .orElse(null))
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


}
