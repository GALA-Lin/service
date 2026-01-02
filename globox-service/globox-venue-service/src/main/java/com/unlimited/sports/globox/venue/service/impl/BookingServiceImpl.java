package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.merchant.entity.VenueBusinessHours;
import com.unlimited.sports.globox.model.merchant.enums.BusinessHourRuleTypeEnum;
import com.unlimited.sports.globox.model.venue.dto.GetCourtSlotsDto;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotRecord;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import com.unlimited.sports.globox.model.venue.enums.BookingSlotStatus;
import com.unlimited.sports.globox.model.venue.enums.CourtStatus;
import com.unlimited.sports.globox.model.venue.enums.CourtType;
import com.unlimited.sports.globox.model.venue.enums.GroundType;
import com.unlimited.sports.globox.model.venue.enums.SlotTypeEnum;
import com.unlimited.sports.globox.model.venue.vo.BookingSlotVo;
import com.unlimited.sports.globox.model.venue.vo.CourtSlotVo;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotRecordMapper;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotTemplateMapper;
import com.unlimited.sports.globox.venue.service.IBookingService;
import com.unlimited.sports.globox.venue.service.IVenueActivityService;
import com.unlimited.sports.globox.venue.service.IVenueBusinessHoursService;
import com.unlimited.sports.globox.venue.service.VenuePriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class BookingServiceImpl implements IBookingService {


    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private CourtMapper courtMapper;

    @Autowired
    private VenueBookingSlotTemplateMapper slotTemplateMapper;

    @Autowired
    private VenueBookingSlotRecordMapper slotRecordMapper;

    @Autowired
    private VenuePriceService venuePriceService;

    @Autowired
    private IVenueActivityService venueActivityService;

    @Autowired
    private IVenueBusinessHoursService venueBusinessHoursService;


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
            throw new GloboxApplicationException("场馆不存在");
        }

        // 时间控制：检查是否允许查看该日期的槽位
        if (!validSlotVisibilityPermission(venue, dto.getBookingDate())) {
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
        };
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

        Map<LocalTime, BigDecimal> priceMap = venuePriceService.getSlotPriceMap(
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
                .map(court -> buildCourtSlotVo(
                        court,
                        venue.getVenueId(),
                        openTime,
                        closeTime,
                        bookingDate,
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
     * 构建场地槽位VO
     *
     * @param court 场地
     * @param venueId 场馆ID（用于价格查询）
     * @param openTime 营业开始时间
     * @param closeTime 营业结束时间
     * @param bookingDate 预订日期
     * @param templates 该场地的所有槽位模板（已查询）
     * @param recordMap 全局的槽位记录映射（模板ID -> 记录）
     * @param priceMap 预先批量获取的价格映射（时间 -> 价格）
     * @param activityMap 全局的活动映射（活动ID -> 活动详情，已查询）
     * @param activityLockedSlots 全局的活动占用槽位映射（槽位模板ID -> 活动ID，已查询）
     * @param userId 当前用户ID，用于判断槽位是否是本人预定的
     * @return 场地槽位VO
     */
    private CourtSlotVo buildCourtSlotVo(
            Court court,
            Long venueId,
            LocalTime openTime,
            LocalTime closeTime,
            LocalDate bookingDate,
            List<VenueBookingSlotTemplate> templates,
            Map<Long, VenueBookingSlotRecord> recordMap,
            Map<LocalTime, BigDecimal> priceMap,
            Map<Long, VenueActivity> activityMap,
            Map<Long, Long> activityLockedSlots,
            Long userId
    ) {

        // 检查是否有槽位模板
        if (templates.isEmpty()) {
            log.warn("场地没有槽位模板: courtId={}, courtName={}, venueId={}, date={}, businessHours={}-{}",
                    court.getCourtId(), court.getName(), venueId, bookingDate, openTime, closeTime);
        }

        // 根据模板生成槽位VO，同一活动只在第一个槽位处显示（使用活动的完整时间段）
        Set<Long> processedActivityIds = new HashSet<>();

        List<BookingSlotVo> slots = templates.stream()
                .flatMap(template -> {
                    Long activityId = activityLockedSlots.get(template.getBookingSlotTemplateId());

                    // 如果被活动占用
                    if (activityId != null) {
                        // 使用 Set.add() 返回值判断是否第一次遇到此活动
                        if (processedActivityIds.add(activityId)) {
                            VenueActivity activity = activityMap.get(activityId);
                            if (activity != null) {
                                return Stream.of(buildActivitySlotVo(activity));
                            }
                        }
                        // 不添加到结果
                        return Stream.empty();
                    }

                    // 普通槽位直接构建返回
                    BookingSlotVo slot = buildBookingSlotVo(
                            template,
                            recordMap.get(template.getBookingSlotTemplateId()),
                            priceMap,
                            activityLockedSlots,
                            activityMap,
                            userId
                    );
                    return Stream.of(slot);
                })
                .collect(Collectors.toList());

        // 构建场地VO
        String courtTypeDesc;
        String groundTypeDesc;
        try {
            courtTypeDesc = CourtType.fromValue(court.getCourtType()).getDescription();
        } catch (Exception e) {
            courtTypeDesc = "";
            log.warn("未知的场地类型: {}", court.getCourtType());
        }

        try {
            groundTypeDesc = GroundType.fromValue(court.getGroundType()).getDescription();
        } catch (Exception e) {
            groundTypeDesc = "";
            log.warn("未知的地面类型: {}", court.getGroundType());
        }

        return CourtSlotVo.builder()
                .courtId(court.getCourtId())
                .courtName(court.getName())
                .courtType(court.getCourtType())
                .courtTypeDesc(courtTypeDesc)
                .groundType(court.getGroundType())
                .groundTypeDesc(groundTypeDesc)
                .slots(slots)
                .build();
    }

    /**
     * 根据槽位模板和记录构建槽位VO，包括价格计算
     *
     * @param template 槽位模板
     * @param record 槽位记录（可能为null，表示尚未创建记录，默认为可预订）
     * @param priceMap 预先批量获取的价格映射（时间 -> 价格）
     * @param activityLockedSlots 活动锁定的槽位映射（槽位模板ID -> 活动ID）
     * @param activityMap 全局的活动映射（活动ID -> 活动详情）
     * @param userId 当前用户ID，用于判断槽位是否是本人预定的
     * @return 槽位VO
     */
    private BookingSlotVo buildBookingSlotVo(
            VenueBookingSlotTemplate template,
            VenueBookingSlotRecord record,
            Map<LocalTime, BigDecimal> priceMap,
            Map<Long, Long> activityLockedSlots,
            Map<Long, VenueActivity> activityMap,
            Long userId
    ) {
        Long templateId = template.getBookingSlotTemplateId();

        // 检查该槽位是否被活动占用
        Long activityId = activityLockedSlots.get(templateId);
        if (activityId != null) {
            // 该槽位被活动占用，使用预先获取的活动信息构建活动槽位VO
            VenueActivity activity = activityMap.get(activityId);
            if (activity != null) {
                return buildActivitySlotVo(activity);
            } else {
                log.warn("活动信息不存在: activityId={}", activityId);
                // 如果活动信息不存在，返回空的活动槽位VO
                return BookingSlotVo.builder()
                        .bookingSlotId(activityId)
                        .slotType(SlotTypeEnum.ACTIVITY.getCode())
                        .startTime(template.getStartTime())
                        .endTime(template.getEndTime())
                        .build();
            }
        }

        // 普通槽位处理逻辑
        // 如果没有记录，则默认为可预订状态
        int status = record != null ? record.getStatus() : BookingSlotStatus.AVAILABLE.getValue();

        // slotId: 如果有记录使用记录ID，否则使用模板ID
        Long slotId = record != null ? record.getBookingSlotRecordId() : templateId;

        String statusDesc;
        boolean isAvailable;
        try {
            BookingSlotStatus statusEnum = BookingSlotStatus.fromValue(status);
            statusDesc = statusEnum.getDescription();
            isAvailable = statusEnum == BookingSlotStatus.AVAILABLE;
        } catch (Exception e) {
            statusDesc = "未知状态";
            isAvailable = false;
            log.error("未知的槽位状态: {}", status);
        }

        // 从预先批量获取的priceMap中获取价格（O(1)查询，无数据库访问）
        BigDecimal price = priceMap.get(template.getStartTime());
        if (price == null) {
            price = BigDecimal.ZERO;
            log.warn("未找到槽位价格: courtId={}, startTime={}", template.getCourtId(), template.getStartTime());
        }

        // 判断是否是本人预定的槽位
        Boolean isMyBooking = record != null && record.getOperatorId() != null && record.getOperatorId().equals(userId);

        return BookingSlotVo.builder()
                .bookingSlotId(slotId)
                .slotType(SlotTypeEnum.NORMAL.getCode())
                .startTime(template.getStartTime())
                .endTime(template.getEndTime())
                .status(status)
                .statusDesc(statusDesc)
                .isAvailable(isAvailable)
                .price(price)
                .isMyBooking(isMyBooking)
                .build();
    }

    /**
     * 根据活动信息构建活动槽位VO
     *
     * 对于被活动占用的槽位，返回活动的完整时间段（activity.startTime - activity.endTime）
     * 而不是单个槽位模板的时间（半小时），确保同一个活动只在第一个槽位处显示一次
     *
     * @param activity 活动信息
     * @return 活动槽位VO（包含所有活动信息）
     */
    private BookingSlotVo buildActivitySlotVo(VenueActivity activity) {
        return BookingSlotVo.builder()
                .bookingSlotId(activity.getActivityId())
                .slotType(SlotTypeEnum.ACTIVITY.getCode())
                .startTime(activity.getStartTime())  // 活动的实际开始时间
                .endTime(activity.getEndTime())      // 活动的实际结束时间
                .activityName(activity.getActivityName())
                .minNtrpLevel(activity.getMinNtrpLevel())
                .currentParticipants(activity.getCurrentParticipants())
                .maxParticipants(activity.getMaxParticipants())
                .unitPrice(activity.getUnitPrice())
                .build();
    }

    /**
     * 检查是否允许查看该日期的槽位
     *
     * 控制逻辑：
     * 1. 检查当前日期是否在允许的预订范围内（不超过maxAdvanceDays）
     * 2. 检查当前时间是否已经到达slotVisibilityTime
     *
     * @param venue 场馆信息
     * @param bookingDate 要查看的预订日期
     * @throws GloboxApplicationException 如果不允许查看则抛出异常
     */
    private boolean validSlotVisibilityPermission(Venue venue, LocalDate bookingDate) {
        LocalDateTime now = java.time.LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        // 获取最大预订提前天数（默认值为7天，如果未设置）
        Integer maxAdvanceDays = venue.getMaxAdvanceDays();
        if (maxAdvanceDays == null || maxAdvanceDays < 0) {
            log.warn("[checkSlotVisibilityPermission]场馆{}: {}未配置预定时间",venue.getVenueId(),venue.getName());
            return false;
        }

        // 获取槽位可见时间点（默认值为00:00，如果未设置）
        LocalTime slotVisibilityTime = venue.getSlotVisibilityTime();
        if (slotVisibilityTime == null) {
            slotVisibilityTime = LocalTime.of(0, 0);
            log.debug("场馆 {} 未配置slotVisibilityTime，使用默认值00:00", venue.getVenueId());
        }

        // 预订日期不能超过最大提前天数
        long daysUntilBooking = ChronoUnit.DAYS.between(today, bookingDate);
        if (daysUntilBooking > maxAdvanceDays) {
            log.warn("用户尝试查看过远的预订日期 - venueId={}, bookingDate={}, 距今{}天，最多允许{}天",
                    venue.getVenueId(), bookingDate, daysUntilBooking, maxAdvanceDays);
            return false;
        }
        // 如果预订日期就是今天，需要检查当前时间是否已经到达开放时间
        if (bookingDate.equals(today)) {
            if (currentTime.isBefore(slotVisibilityTime)) {
                log.warn("用户尝试查看今日槽位，但还未到开放时间 - venueId={}, 当前时间={}, 开放时间={}",
                        venue.getVenueId(), currentTime, slotVisibilityTime);
                
                return false;
            }
        }
        return true;
    }


}
