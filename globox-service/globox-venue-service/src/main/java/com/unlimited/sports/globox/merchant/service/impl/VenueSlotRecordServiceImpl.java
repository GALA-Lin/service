package com.unlimited.sports.globox.merchant.service.impl;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.merchant.mapper.*;
import com.unlimited.sports.globox.merchant.service.VenueSlotRecordService;
import com.unlimited.sports.globox.model.merchant.entity.*;
import com.unlimited.sports.globox.model.merchant.enums.BusinessHourRuleTypeEnum;
import com.unlimited.sports.globox.model.merchant.enums.DateTypeEnum;
import com.unlimited.sports.globox.model.merchant.enums.SlotRecordStatusEnum;
import com.unlimited.sports.globox.model.merchant.vo.SlotAvailabilityVo;
import com.unlimited.sports.globox.model.merchant.vo.SlotGenerationResultVo;
import com.unlimited.sports.globox.model.merchant.vo.VenueSlotAvailabilityVo;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplatePeriod;
import com.unlimited.sports.globox.model.venue.enums.BookingSlotStatus;
import com.unlimited.sports.globox.model.venue.enums.CourtStatus;
import com.unlimited.sports.globox.model.venue.vo.BookingSlotVo;
import com.unlimited.sports.globox.venue.mapper.VenueActivityMapper;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplatePeriodMapper;
import com.unlimited.sports.globox.venue.service.IVenueActivityService;
import com.unlimited.sports.globox.venue.service.IVenuePriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 2025/12/27 15:23
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VenueSlotRecordServiceImpl implements VenueSlotRecordService {

    private final MerchantVenueBookingSlotTemplateMapper templateMapper;
    private final MerchantVenueBookingSlotRecordMapper recordMapper;
    private final CourtMapper courtMapper;
    private final VenueMapper venueMapper;
    private final VenueBusinessHoursMapper businessHoursMapper;
    private final VenuePriceTemplatePeriodMapper priceTemplatePeriodMapper;

    private final IVenuePriceService venuePriceService;
    private final IVenueActivityService venueActivityService;
    private final VenueActivityMapper venueActivityMapper;

    /**
     * 为指定日期生成槽位记录
     *
     * @param courtId   场地ID
     * @param date      日期
     * @param overwrite 是否覆盖已有记录
     * @return 生成结果
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SlotGenerationResultVo generateRecordsForDate(Long courtId, LocalDate date, boolean overwrite) {
        // 验证场地
        Court court = courtMapper.selectById(courtId);
        if (court == null) {
            throw new GloboxApplicationException("场地不存在");
        }

        // 获取场馆信息
        Venue venue = venueMapper.selectById(court.getVenueId());
        if (venue == null) {
            throw new GloboxApplicationException("场馆不存在");
        }

        // 检查营业时间
        VenueBusinessHours businessHours = getBusinessHours(venue.getVenueId(), date);
        if (businessHours == null) {
            return SlotGenerationResultVo.builder()
                    .success(false)
                    .totalDays(1)
                    .totalSlots(0)
                    .skippedDays(1)
                    .message("该日期不营业")
                    .build();
        }

        // 获取模板
        List<VenueBookingSlotTemplate> templates = templateMapper.selectByCourtIdOrderByTime(courtId);
        if (templates.isEmpty()) {
            throw new GloboxApplicationException("场地尚未初始化时段模板，请先创建模板");
        }

        // 筛选在营业时间内的模板
        List<VenueBookingSlotTemplate> validTemplates = templates.stream()
                .filter(t -> !t.getStartTime().isBefore(businessHours.getOpenTime())
                        && !t.getEndTime().isAfter(businessHours.getCloseTime()))
                .toList();

        // 检查是否已有记录
        Integer existingCount = recordMapper.countByCourtAndDate(courtId, date);
        if (existingCount > 0 && !overwrite) {
            return SlotGenerationResultVo.builder()
                    .success(false)
                    .totalDays(1)
                    .totalSlots(0)
                    .skippedDays(1)
                    .message("该日期已有记录，如需重新生成请选择覆盖模式")
                    .build();
        }

        // 如果覆盖模式，先删除旧记录
        if (overwrite && existingCount > 0) {
            validTemplates.forEach(template -> {
                recordMapper.deleteAvailableByTemplateAndDateRange(
                        template.getBookingSlotTemplateId(), date, date);
            });
        }

        // 批量创建记录
        List<VenueBookingSlotRecord> records = validTemplates.stream()
                .map(template -> VenueBookingSlotRecord.builder()
                        .slotTemplateId(template.getBookingSlotTemplateId())
                        .bookingDate(date)
                        .status(SlotRecordStatusEnum.AVAILABLE.getCode())
                        .build())
                .collect(Collectors.toList());

        if (records.isEmpty()) {
            log.info("没有可生成的时段记录，跳过插入");
            return SlotGenerationResultVo.builder().success(true).totalSlots(0).build();
        }

        int count = recordMapper.batchInsert(records);

        log.info("场地ID: {} 日期: {} 生成了 {} 条槽位记录", courtId, date, count);

        return SlotGenerationResultVo.builder()
                .success(true)
                .totalDays(1)
                .totalSlots(count)
                .skippedDays(0)
                .message("生成成功")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SlotGenerationResultVo generateRecordsForDateRange(Long courtId, LocalDate startDate,
                                                              LocalDate endDate, boolean overwrite) {
        if (startDate.isAfter(endDate)) {
            throw new GloboxApplicationException("开始日期不能晚于结束日期");
        }
        int totalDays = 0;
        int totalSlots = 0;
        int skippedDays = 0;
        List<SlotGenerationResultVo.DailySlotInfo> dailyInfoList = new ArrayList<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            totalDays++;

            SlotGenerationResultVo result = generateRecordsForDate(courtId, currentDate, overwrite);
            totalSlots += result.getTotalSlots();

            if (!result.getSuccess() || result.getSkippedDays() > 0) {
                skippedDays++;
                dailyInfoList.add(SlotGenerationResultVo.DailySlotInfo.builder()
                        .date(currentDate)
                        .slotCount(0)
                        .status("skipped")
                        .remark(result.getMessage())
                        .build());
            } else {
                dailyInfoList.add(SlotGenerationResultVo.DailySlotInfo.builder()
                        .date(currentDate)
                        .slotCount(result.getTotalSlots())
                        .status("success")
                        .remark("生成成功")
                        .build());
            }

            currentDate = currentDate.plusDays(1);
        }

        return SlotGenerationResultVo.builder()
                .success(true)
                .totalDays(totalDays)
                .totalSlots(totalSlots)
                .skippedDays(skippedDays)
                .dailySlotInfoList(dailyInfoList)
                .message(String.format("批量生成完成，共处理%d天，生成%d个时段", totalDays, totalSlots))
                .build();
    }

    /**
     * 查询某天的时段可用性
     *
     * @param courtId 场地ID
     * @param date    日期
     * @return 时段列表
     */
    @Override
    public List<SlotAvailabilityVo> queryAvailability(Long courtId, LocalDate date) {
        // 1. 获取模板
        List<VenueBookingSlotTemplate> templates = templateMapper.selectByCourtIdOrderByTime(courtId);
        if (templates.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 查询当天的记录（一次性查询）
        List<Long> templateIds = templates.stream()
                .map(VenueBookingSlotTemplate::getBookingSlotTemplateId)
                .collect(Collectors.toList());

        Map<Long, VenueBookingSlotRecord> recordMap = recordMapper
                .selectByTemplateIdsAndDate(templateIds, date)
                .stream()
                .collect(Collectors.toMap(VenueBookingSlotRecord::getSlotTemplateId, r -> r));

        // 3. 获取价格信息
        Court court = courtMapper.selectById(courtId);
        Venue venue = venueMapper.selectById(court.getVenueId());
        List<VenuePriceTemplatePeriod> pricePeriods = priceTemplatePeriodMapper
                .selectByTemplateId(venue.getTemplateId());

        // 4. 组装结果
        return templates.stream()
                .map(template -> {
                    VenueBookingSlotRecord record = recordMap.get(template.getBookingSlotTemplateId());

                    SlotAvailabilityVo vo = SlotAvailabilityVo.builder()
                            .startTime(template.getStartTime())
                            .endTime(template.getEndTime())
                            .templateId(template.getBookingSlotTemplateId())
                            .build();

                    if (record == null) {
                        // 未生成记录 = 可预订
                        vo.setAvailable(true);
                        vo.setStatus(1);
                        vo.setPrice(calculatePrice(template.getStartTime(), pricePeriods, date));
                        vo.setStatusRemark("可预订");
                    } else {
                        vo.setBookingSlotId(record.getBookingSlotRecordId());
                        vo.setStatus(record.getStatus());
                        vo.setAvailable(record.getStatus().equals(SlotRecordStatusEnum.AVAILABLE.getCode()));
                        vo.setPrice(calculatePrice(template.getStartTime(), pricePeriods, date));
                        vo.setStatusRemark(getStatusName(record.getStatus()));
                        vo.setLockedType(record.getLockedType());
                        vo.setLockReason(record.getLockReason());
                        vo.setOrderId(String.valueOf(record.getOrderId()));
                    }

                    return vo;
                })
                .collect(Collectors.toList());
    }
    /**
     * 查询场馆下所有场地某日的时段可用性（增强版 - 支持活动）
     *
     * @param venueId   场馆ID
     * @param date      日期
     * @param startTime 开始时间（可选，筛选时间范围）
     * @param endTime   结束时间（可选，筛选时间范围）
     * @return 场馆级时段可用性列表（按场地分组）
     */
    @Override
    public List<VenueSlotAvailabilityVo> queryVenueAvailability(Long venueId, LocalDate date,
                                                                LocalTime startTime, LocalTime endTime) {
        // 1. 验证场馆
        Venue venue = venueMapper.selectById(venueId);
        if (venue == null) {
            throw new GloboxApplicationException("场馆不存在");
        }

        // 2. 获取营业时间
        VenueBusinessHours businessHours = getBusinessHours(venueId, date);
        if (businessHours == null) {
            log.warn("场馆未配置营业时间或当天不营业 - venueId: {}, date: {}", venueId, date);
            return Collections.emptyList();
        }

        if (BusinessHourRuleTypeEnum.CLOSED_DATE.getCode().equals(businessHours.getRuleType())) {
            log.info("场馆当天不开放 - venueId: {}, date: {}", venueId, date);
            return Collections.emptyList();
        }

        // 3. 查询场馆下的所有开放场地
        List<Court> courts = courtMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Court>()
                        .eq(Court::getVenueId, venueId)
                        .eq(Court::getStatus, CourtStatus.OPEN.getValue())
                        .orderByAsc(Court::getCourtId)
        );

        if (courts.isEmpty()) {
            log.warn("场馆没有开放的场地 - venueId: {}", venueId);
            return Collections.emptyList();
        }

        LocalTime openTime = businessHours.getOpenTime();
        LocalTime closeTime = businessHours.getCloseTime();
        List<Long> courtIds = courts.stream().map(Court::getCourtId).toList();

        // 4. 批量查询所有场地的槽位模板（使用venue的mapper以支持活动）
        List<VenueBookingSlotTemplate> allTemplates = templateMapper.MerchantSelectByCourtIdsAndTimeRange(
                courtIds,
                openTime,
                closeTime
        );

        // 5. 批量查询所有场地该日期的槽位记录
        List<VenueBookingSlotRecord> allRecords = recordMapper.MerchantSelectByCourtIdsAndDate(
                courtIds,
                date
        );

        // 6. 构建映射表
        Map<Long, List<VenueBookingSlotTemplate>> templatesByCourtId = allTemplates.stream()
                .collect(Collectors.groupingBy(VenueBookingSlotTemplate::getCourtId));

        Map<Long, VenueBookingSlotRecord> recordMap = allRecords.stream()
                .collect(Collectors.toMap(VenueBookingSlotRecord::getSlotTemplateId, record -> record));

        // 7. 批量获取所有槽位的价格
        List<LocalTime> slotStartTimes = allTemplates.stream()
                .map(VenueBookingSlotTemplate::getStartTime)
                .distinct()
                .collect(Collectors.toList());

        if (venue.getTemplateId() == null) {
            throw new GloboxApplicationException("场馆价格未配置");
        }

        Map<LocalTime, BigDecimal> priceMap = venuePriceService.getSlotPriceMap(
                venue.getTemplateId(),
                venue.getVenueId(),
                date,
                slotStartTimes
        );

        // 8. 批量获取场馆在该日期的所有活动
        List<VenueActivity> allActivities = venueActivityService.getActivitiesByVenueAndDate(
                venue.getVenueId(),
                date
        );

        // 构建活动ID映射表
        Map<Long, VenueActivity> activityMap = allActivities.stream()
                .collect(Collectors.toMap(VenueActivity::getActivityId, activity -> activity));

        // 批量获取所有活动占用的槽位映射
        List<Long> allActivityIds = allActivities.stream()
                .map(VenueActivity::getActivityId)
                .collect(Collectors.toList());

        Map<Long, Long> activityLockedSlots = allActivityIds.isEmpty() ?
                Map.of() :
                venueActivityService.getActivityLockedSlotsByIds(allActivityIds, date);

        // 9. 为每个场地构建槽位可用性信息
        List<VenueSlotAvailabilityVo> result = new ArrayList<>();

        for (Court court : courts) {
            List<VenueBookingSlotTemplate> courtTemplates =
                    templatesByCourtId.getOrDefault(court.getCourtId(), Collections.emptyList());

            if (courtTemplates.isEmpty()) {
                continue; // 跳过没有槽位模板的场地
            }

            // 转换为 BookingSlotVo 列表（包含活动信息）
            List<BookingSlotVo> slots = courtTemplates.stream()
                    .map(template -> buildBookingSlotVo(
                            template,
                            recordMap,
                            priceMap,
                            activityMap,
                            activityLockedSlots,
                            null // 商家端不需要判断是否是用户自己的预订
                    ))
                    .collect(Collectors.toList());

            // 如果指定了时间范围，进行过滤
            if (startTime != null || endTime != null) {
                slots = filterSlotsByTimeRange(slots, startTime, endTime);
            }

            // 统计可用时段数量
            long availableCount = slots.stream()
                    .filter(BookingSlotVo::getIsAvailable)
                    .count();

            // 转换为 SlotAvailabilityVo 格式（兼容原接口）
            List<SlotAvailabilityVo> merchantSlots = convertToSlotAvailabilityVo(slots);

            // 构建场地级VO
            VenueSlotAvailabilityVo courtVo = VenueSlotAvailabilityVo.builder()
                    .courtId(court.getCourtId())
                    .courtName(court.getName())
                    .courtType(getCourtTypeName(court.getCourtType()))
                    .slots(merchantSlots)
                    .availableCount((int) availableCount)
                    .totalCount(slots.size())
                    .build();

            result.add(courtVo);
        }

        log.info("场馆ID: {} 日期: {} 查询到 {} 个场地的时段信息", venueId, date, result.size());
        return result;
    }

    /**
     * 构建 BookingSlotVo（复用用户端逻辑）
     */
    private BookingSlotVo buildBookingSlotVo(
            VenueBookingSlotTemplate template,
            Map<Long, VenueBookingSlotRecord> recordMap,
            Map<LocalTime, BigDecimal> priceMap,
            Map<Long, VenueActivity> activityMap,
            Map<Long, Long> activityLockedSlots,
            Long userId) {

        VenueBookingSlotRecord record = recordMap.get(template.getBookingSlotTemplateId());

        // 检查是否被活动占用
        Long activityId = activityLockedSlots.get(template.getBookingSlotTemplateId());
        boolean isActivitySlot = activityId != null;

        // 如果是活动槽位，获取活动详情
        VenueActivity activity = isActivitySlot ? activityMap.get(activityId) : null;

        BigDecimal price = priceMap.getOrDefault(template.getStartTime(), BigDecimal.ZERO);

        // 如果是活动槽位且有单价，使用活动价格
        if (isActivitySlot && activity != null && activity.getUnitPrice() != null) {
            price = activity.getUnitPrice();
        }

        // 确定槽位状态
        Integer status;
        String statusDesc;
        boolean isAvailable;

        if (isActivitySlot) {
            // 活动槽位逻辑
            if (activity == null) {
                status = BookingSlotStatus.EXPIRED.getValue();
                statusDesc = "活动已失效";
                isAvailable = false;
            } else if (activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
                status = BookingSlotStatus.LOCKED_IN.getValue();
                statusDesc = "活动已满员";
                isAvailable = false;
            } else {
                status = BookingSlotStatus.AVAILABLE.getValue();
                statusDesc = "可报名";
                isAvailable = true;
            }
        } else if (record == null) {
            // 未生成记录 = 可预订
            status = BookingSlotStatus.AVAILABLE.getValue();
            statusDesc = BookingSlotStatus.AVAILABLE.getDescription();
            isAvailable = true;
        } else {
            // 普通槽位，根据记录状态判断
            status = record.getStatus();
            BookingSlotStatus slotStatus = BookingSlotStatus.fromValue(status);
            statusDesc = slotStatus.getDescription();
            isAvailable = BookingSlotStatus.AVAILABLE.getValue() == status;
        }

        return BookingSlotVo.builder()
                .bookingSlotId(template.getBookingSlotTemplateId())
                .startTime(template.getStartTime())
                .endTime(template.getEndTime())
                .slotType(isActivitySlot ?
                        com.unlimited.sports.globox.model.venue.enums.SlotTypeEnum.ACTIVITY.getCode() :
                        com.unlimited.sports.globox.model.venue.enums.SlotTypeEnum.NORMAL.getCode())
                .status(status)
                .statusDesc(statusDesc)
                .isAvailable(isAvailable)
                .price(price)
                .isMyBooking(false) // 商家端不需要此字段
                .activityName(activity != null ? activity.getActivityName() : null)
                .currentParticipants(activity != null ? activity.getCurrentParticipants() : null)
                .maxParticipants(activity != null ? activity.getMaxParticipants() : null)
                .build();
    }

    /**
     * 转换 BookingSlotVo 为 SlotAvailabilityVo（兼容商家端接口）
     */
    private List<SlotAvailabilityVo> convertToSlotAvailabilityVo(List<BookingSlotVo> bookingSlots) {
        return bookingSlots.stream()
                .map(slot -> SlotAvailabilityVo.builder()
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .templateId(slot.getBookingSlotId())
                        .bookingSlotId(slot.getBookingSlotId())
                        .status(slot.getStatus())
                        .available(slot.getIsAvailable())
                        .price(slot.getPrice())
                        .statusRemark(slot.getStatusDesc())
                        // 活动相关字段
                        .slotType(slot.getSlotType())
                        .activityName(slot.getActivityName())
                        .currentParticipants(slot.getCurrentParticipants())
                        .maxParticipants(slot.getMaxParticipants())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 根据时间范围过滤槽位（BookingSlotVo 版本）
     */
    private List<BookingSlotVo> filterSlotsByTimeRange(List<BookingSlotVo> slots,
                                                       LocalTime startTime, LocalTime endTime) {
        return slots.stream()
                .filter(slot -> {
                    if (startTime != null && slot.getStartTime().isBefore(startTime)) {
                        return false;
                    }
                    if (endTime != null && slot.getEndTime().isAfter(endTime)) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取场地类型名称
     */
    private String getCourtTypeName(Integer courtType) {
        if (courtType == null) {
            return "未知";
        }
        // 这里根据实际的枚举来映射，示例：
        return switch (courtType) {
            case 1 -> "红土";
            case 2 -> "硬地";
            case 3 -> "草地";
            default -> "其他";
        };
    }

    /**
     * 获取营业时间
     */
    private VenueBusinessHours getBusinessHours(Long venueId, LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        List<VenueBusinessHours> rules = businessHoursMapper.selectByVenueIdAndDate(venueId, date, dayOfWeek);

        if (rules.isEmpty()) {
            return getDefaultBusinessHours(venueId);
        }

        // 优先级：关闭日期 > 特殊日期 > 常规规则
        for (VenueBusinessHours rule : rules) {
            if (BusinessHourRuleTypeEnum.CLOSED_DATE.getCode().equals(rule.getRuleType())) {
                return null;
            }
        }

        for (VenueBusinessHours rule : rules) {
            if (BusinessHourRuleTypeEnum.SPECIAL_DATE.getCode().equals(rule.getRuleType())) {
                return rule;
            }
        }

        return rules.stream()
                .filter(r -> BusinessHourRuleTypeEnum.REGULAR.getCode().equals(r.getRuleType()))
                .findFirst()
                .orElse(getDefaultBusinessHours(venueId));
    }

    /**
     * 默认营业时间
     */
    private VenueBusinessHours getDefaultBusinessHours(Long venueId) {
        VenueBusinessHours defaultHours = new VenueBusinessHours();
        defaultHours.setVenueId(venueId);
        defaultHours.setOpenTime(LocalTime.of(6, 0));
        defaultHours.setCloseTime(LocalTime.of(23, 0));
        return defaultHours;
    }

    /**
     * 计算价格
     */
    private BigDecimal calculatePrice(LocalTime startTime, List<VenuePriceTemplatePeriod> pricePeriods, LocalDate date) {
        if (pricePeriods == null || pricePeriods.isEmpty()) {
            return BigDecimal.valueOf(100); // 默认价格
        }

        DateTypeEnum dateType = DateTypeEnum.getDateType(date);

        for (VenuePriceTemplatePeriod period : pricePeriods) {
            if (!startTime.isBefore(period.getStartTime()) && startTime.isBefore(period.getEndTime())) {
                return switch (dateType) {
                    case WEEKDAY -> period.getWeekdayPrice();
                    case WEEKEND -> period.getWeekendPrice();
                    case HOLIDAY -> period.getHolidayPrice();
                };
            }
        }

        return BigDecimal.valueOf(100);
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer statusCode) {
        SlotRecordStatusEnum status = SlotRecordStatusEnum.getByCode(statusCode);
        return status != null ? status.getName() : "未知";
    }
}
