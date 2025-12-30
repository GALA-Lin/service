package com.unlimited.sports.globox.merchant.service.impl;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.merchant.mapper.*;
import com.unlimited.sports.globox.merchant.service.VenueSlotRecordService;
import com.unlimited.sports.globox.model.merchant.entity.*;
import com.unlimited.sports.globox.model.merchant.enums.BusinessHourRuleTypeEnum;
import com.unlimited.sports.globox.model.merchant.enums.DateTypeEnum;
import com.unlimited.sports.globox.model.merchant.enums.LockTypeEnum;
import com.unlimited.sports.globox.model.merchant.enums.SlotRecordStatusEnum;
import com.unlimited.sports.globox.model.merchant.vo.SlotAvailabilityVo;
import com.unlimited.sports.globox.model.merchant.vo.SlotGenerationResultVo;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplatePeriod;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplatePeriodMapper;
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

import static com.unlimited.sports.globox.model.merchant.enums.LockTypeEnum.*;
import static com.unlimited.sports.globox.model.merchant.enums.SlotRecordStatusEnum.AVAILABLE;
import static com.unlimited.sports.globox.model.merchant.enums.SlotRecordStatusEnum.UNAVAILABLE;

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
                        .status(AVAILABLE.getCode())
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
//
//    /**
//     * 查询某天的时段可用性
//     *
//     * @param courtId 场地ID
//     * @param date    日期
//     * @return 时段列表
//     */
//    @Override
//    public List<SlotAvailabilityVo> queryAvailability(Long courtId, LocalDate date) {
//        // 1. 获取模板
//        List<VenueBookingSlotTemplate> templates = templateMapper.selectByCourtIdOrderByTime(courtId);
//        if (templates.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        // 2. 查询当天的记录（一次性查询）
//        List<Long> templateIds = templates.stream()
//                .map(VenueBookingSlotTemplate::getBookingSlotTemplateId)
//                .collect(Collectors.toList());
//
//        Map<Long, VenueBookingSlotRecord> recordMap = recordMapper
//                .selectByTemplateIdsAndDate(templateIds, date)
//                .stream()
//                .collect(Collectors.toMap(VenueBookingSlotRecord::getSlotTemplateId, r -> r));
//
//        // 3. 获取价格信息
//        Court court = courtMapper.selectById(courtId);
//        Venue venue = venueMapper.selectById(court.getVenueId());
//        List<VenuePriceTemplatePeriod> pricePeriods = priceTemplatePeriodMapper
//                .selectByTemplateId(venue.getTemplateId());
//
//        // 4. 组装结果
//        return templates.stream()
//                .map(template -> {
//                    VenueBookingSlotRecord record = recordMap.get(template.getBookingSlotTemplateId());
//
//                    SlotAvailabilityVo vo = SlotAvailabilityVo.builder()
//                            .startTime(template.getStartTime())
//                            .endTime(template.getEndTime())
//                            .build();
//
//                    if (record == null) {
//                        // 未生成记录 = 不可预订
//                        vo.setAvailable(false);
//                        vo.setPrice(BigDecimal.ZERO);
//                        vo.setStatusRemark("未开放");
//                    } else {
//                        vo.setBookingSlotId(record.getBookingSlotRecordId());
//                        vo.setAvailable(record.getStatus().equals(SlotRecordStatusEnum.AVAILABLE.getCode()));
//                        vo.setPrice(calculatePrice(template.getStartTime(), pricePeriods, date));
//                        vo.setStatusRemark(getStatusName(record.getStatus()));
//                    }
//
//                    return vo;
//                })
//                .collect(Collectors.toList());
//    }

    /**
     * 查询某天的时段可用性
     * 重要逻辑：
     * 1. 如果记录不存在，且在营业时间内 => 可预订
     * 2. 如果记录存在且状态为AVAILABLE => 可预订
     * 3. 如果记录存在且状态为UNAVAILABLE => 不可预订（显示锁定原因）
     *
     * @param courtId 场地ID
     * @param date    日期
     * @return 时段列表
     */
    @Override
    public List<SlotAvailabilityVo> queryAvailability(Long courtId, LocalDate date) {
        // 1. 验证场地和场馆
        Court court = courtMapper.selectById(courtId);
        if (court == null) {
            throw new GloboxApplicationException("场地不存在");
        }

        Venue venue = venueMapper.selectById(court.getVenueId());
        if (venue == null) {
            throw new GloboxApplicationException("场馆不存在");
        }

        // 2. 检查营业时间
        VenueBusinessHours businessHours = getBusinessHours(venue.getVenueId(), date);
        if (businessHours == null) {
            log.info("场地ID: {} 在日期: {} 不营业", courtId, date);
            return Collections.emptyList();
        }

        // 3. 获取模板
        List<VenueBookingSlotTemplate> templates = templateMapper.selectByCourtIdOrderByTime(courtId);
        if (templates.isEmpty()) {
            log.warn("场地ID: {} 尚未配置时段模板", courtId);
            return Collections.emptyList();
        }

        // 4. 筛选在营业时间内的模板
        List<VenueBookingSlotTemplate> validTemplates = templates.stream()
                .filter(t -> !t.getStartTime().isBefore(businessHours.getOpenTime())
                        && !t.getEndTime().isAfter(businessHours.getCloseTime()))
                .toList();

        // 5. 查询当天的记录（一次性查询）
        List<Long> templateIds = validTemplates.stream()
                .map(VenueBookingSlotTemplate::getBookingSlotTemplateId)
                .collect(Collectors.toList());

        Map<Long, VenueBookingSlotRecord> recordMap = recordMapper
                .selectByTemplateIdsAndDate(templateIds, date)
                .stream()
                .collect(Collectors.toMap(VenueBookingSlotRecord::getSlotTemplateId, r -> r));

        // 6. 获取价格信息
        List<VenuePriceTemplatePeriod> pricePeriods = Collections.emptyList();
        if (venue.getTemplateId() != null) {
            pricePeriods = priceTemplatePeriodMapper.selectByTemplateId(venue.getTemplateId());
        }

        // 7. 组装结果
        List<VenuePriceTemplatePeriod> finalPricePeriods = pricePeriods;
        return validTemplates.stream()
                .map(template -> {
                    VenueBookingSlotRecord record = recordMap.get(template.getBookingSlotTemplateId());
                    return buildSlotAvailability(template, record, finalPricePeriods, date);
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建时段可用性信息
     * 核心逻辑：
     * 1. 没有记录 = 可预订
     * 2. 有记录且状态为 AVAILABLE = 可预订
     * 3. 有记录且状态为 UNAVAILABLE = 不可预订
     */
    private SlotAvailabilityVo buildSlotAvailability(
            VenueBookingSlotTemplate template,
            VenueBookingSlotRecord record,
            List<VenuePriceTemplatePeriod> pricePeriods,
            LocalDate date) {

        // 判断是否可预订
        boolean isAvailable = isSlotAvailable(record);
        Integer status = isAvailable ? AVAILABLE.getCode() : UNAVAILABLE.getCode();
        // 构建返回对象
        return SlotAvailabilityVo.builder()
                .bookingSlotId(record != null ? record.getBookingSlotRecordId() : null)
                .templateId(template.getBookingSlotTemplateId())
                .startTime(template.getStartTime())
                .endTime(template.getEndTime())
                .available(isAvailable)
                .price(calculatePrice(template.getStartTime(), pricePeriods, date))
                .status(status)
                .statusRemark(isAvailable ? "可预订" : buildUnavailableRemark(record))
                .build();
    }

    /**
     * 判断时段是否可预订
     * 无记录或状态为AVAILABLE = 可预订
     */
    private boolean isSlotAvailable(VenueBookingSlotRecord record) {
        return record == null ||
                record.getStatus().equals(AVAILABLE.getCode());
    }

    /**
     * 构建不可用时段的说明
     */
    private String buildUnavailableRemark(VenueBookingSlotRecord record) {
        if (record == null) {
            return "可预订"; // 理论上不会走到这里
        }

        if (record.getLockedType() != null) {
            LockTypeEnum lockType = LockTypeEnum.getByCode(record.getLockedType());
            if (lockType != null) {
                return switch (lockType) {
                    case USER_ORDER -> record.getOrderId() != null
                            ? "已被预订（订单ID: " + record.getOrderId() + "）"
                            : "已被预订";
                    case MERCHANT_LOCK -> record.getLockReason() != null
                            ? "商家锁场（" + record.getLockReason() + "）"
                            : "商家锁场";
                    default -> "不可预订";
                };
            }
        }
        return getStatusName(record.getStatus());
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
