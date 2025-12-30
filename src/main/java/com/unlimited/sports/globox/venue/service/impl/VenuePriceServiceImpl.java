package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.enums.order.ChargeModeEnum;
import com.unlimited.sports.globox.dubbo.merchant.dto.ExtraQuote;
import com.unlimited.sports.globox.dubbo.merchant.dto.OrderLevelExtraQuote;
import com.unlimited.sports.globox.dubbo.merchant.dto.SlotQuote;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplate;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplatePeriod;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueExtraChargeTemplate;
import com.unlimited.sports.globox.model.venue.enums.DayType;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplatePeriodMapper;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplateMapper;
import com.unlimited.sports.globox.venue.mapper.VenueExtraChargeTemplateMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.venue.service.VenuePriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 场馆价格服务实现
 * 统一处理价格相关的所有查询和计算
 */
@Slf4j
@Service
public class VenuePriceServiceImpl implements VenuePriceService {

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private VenuePriceTemplateMapper priceTemplateMapper;

    @Autowired
    private VenuePriceTemplatePeriodMapper priceTemplatePeriodMapper;

    @Autowired
    private VenueExtraChargeTemplateMapper extraChargeTemplateMapper;

    /**
     * 查询单个时间点的价格
     *
     * @param venueId 场馆ID
     * @param bookingDate 预订日期
     * @param slotTime 槽位时间（开始时间）
     * @return 价格，如果未找到则返回 null
     */
    @Override
    public BigDecimal getSlotPrice(Long venueId, LocalDate bookingDate, LocalTime slotTime) {
        Map<LocalTime, BigDecimal> priceMap = getSlotPriceMap(venueId, bookingDate, Collections.singletonList(slotTime));
        return priceMap.get(slotTime);
    }

    /**
     * 批量查询多个时间点的价格
     * 通过构建内部缓存，避免重复查询
     *
     * @param venueId 场馆ID
     * @param bookingDate 预订日期
     * @param slotTimes 槽位时间列表（开始时间）
     * @return 时间 -> 价格 的映射
     */
    @Override
    public Map<LocalTime, BigDecimal> getSlotPriceMap(Long venueId, LocalDate bookingDate, List<LocalTime> slotTimes) {
        Map<LocalTime, BigDecimal> priceMap = new HashMap<>();

        if (slotTimes == null || slotTimes.isEmpty()) {
            return priceMap;
        }

        // 获取场馆信息
        Venue venue = venueMapper.selectById(venueId);
        if (venue == null || venue.getTemplateId() == null) {
            log.debug("场馆未配置或未设置价格模板: venueId={}", venueId);
            return priceMap;
        }

        // 获取价格模板
        VenuePriceTemplate template = priceTemplateMapper.selectById(venue.getTemplateId());
        if (template == null || !template.getIsEnabled()) {
            log.warn("价格模板不存在或未启用: templateId={}", venue.getTemplateId());
            return priceMap;
        }

        // 获取价格时段
        List<VenuePriceTemplatePeriod> periods = priceTemplatePeriodMapper.selectList(
                new LambdaQueryWrapper<VenuePriceTemplatePeriod>()
                        .eq(VenuePriceTemplatePeriod::getTemplateId, template.getTemplateId())
                        .eq(VenuePriceTemplatePeriod::getIsEnabled, true)
                        .orderByAsc(VenuePriceTemplatePeriod::getStartTime)
        );

        if (periods.isEmpty()) {
            log.warn("价格模板未配置时段: templateId={}", template.getTemplateId());
            return priceMap;
        }

        // 确定日期类型
        DayType dayType = determineDayType(bookingDate);
        log.debug("计算价格: venueId={}, bookingDate={}, dayType={}, slotCount={}",
                venueId, bookingDate, dayType, slotTimes.size());

        // 为每个槽位时间查找价格
        for (LocalTime slotTime : slotTimes) {
            for (VenuePriceTemplatePeriod period : periods) {
                // 检查槽位时间是否在该时段内 [startTime, endTime)
                if (isTimeInPeriod(slotTime, period.getStartTime(), period.getEndTime())) {
                    BigDecimal price = selectPriceByDayType(period, dayType);
                    if (price != null) {
                        priceMap.put(slotTime, price);
                        log.debug("找到价格: time={}, price={}", slotTime, price);
                    }
                    break; // 找到后就跳出，不再查找其他时段
                }
            }
        }

        log.debug("价格查询完成: 共{}个时间，找到{}个价格", slotTimes.size(), priceMap.size());
        return priceMap;
    }

    /**
     * 批量查询多个时间点的价格（字符串版本）
     *
     * @param venueId 场馆ID
     * @param bookingDate 预订日期
     * @param slotTimeStrings 槽位时间列表（字符串格式：HH:mm:ss）
     * @return 时间字符串 -> 价格 的映射
     */
    @Override
    public Map<String, BigDecimal> getSlotPriceMapByString(Long venueId, LocalDate bookingDate, List<String> slotTimeStrings) {
        Map<String, BigDecimal> result = new HashMap<>();

        if (slotTimeStrings == null || slotTimeStrings.isEmpty()) {
            return result;
        }

        // 将字符串转换为 LocalTime
        List<LocalTime> slotTimes = slotTimeStrings.stream()
                .map(LocalTime::parse)
                .collect(Collectors.toList());

        // 获取 LocalTime 版本的价格 Map
        Map<LocalTime, BigDecimal> priceMap = getSlotPriceMap(venueId, bookingDate, slotTimes);

        // 转换回字符串版本
        priceMap.forEach((time, price) -> result.put(time.toString(), price));

        return result;
    }

    /**
     * 获取价格模板
     *
     * @param venueId 场馆ID
     * @return 价格模板，如果未配置则返回 null
     */
    @Override
    public VenuePriceTemplate getPriceTemplate(Long venueId) {
        Venue venue = venueMapper.selectById(venueId);
        if (venue == null || venue.getTemplateId() == null) {
            log.debug("场馆未配置价格模板: venueId={}", venueId);
            return null;
        }

        VenuePriceTemplate template = priceTemplateMapper.selectById(venue.getTemplateId());
        if (template == null) {
            log.warn("价格模板不存在: templateId={}", venue.getTemplateId());
            return null;
        }

        if (!template.getIsEnabled()) {
            log.debug("价格模板未启用: templateId={}", venue.getTemplateId());
            return null;
        }

        return template;
    }

    /**
     * 判断日期类型（工作日/周末/节假日）
     *
     * @param date 日期
     * @return 日期类型
     */
    @Override
    public DayType determineDayType(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // TODO: 后续可以添加节假日判断逻辑
        // 暂时只区分工作日和周末
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return DayType.WEEKEND;
        } else {
            return DayType.WEEKDAY;
        }
    }

    /**
     * 判断时间是否在时段内 [startTime, endTime)
     *
     * @param time 槽位时间
     * @param startTime 时段开始时间（包含）
     * @param endTime 时段结束时间（不包含）
     * @return 若时间在时段内返回true
     */
    private boolean isTimeInPeriod(LocalTime time, LocalTime startTime, LocalTime endTime) {
        return !time.isBefore(startTime) && time.isBefore(endTime);
    }

    /**
     * 根据日期类型选择价格
     *
     * @param period 价格时段
     * @param dayType 日期类型
     * @return 对应的价格
     */
    private BigDecimal selectPriceByDayType(VenuePriceTemplatePeriod period, DayType dayType) {
        return switch (dayType) {
            case HOLIDAY -> period.getHolidayPrice();
            case WEEKEND -> period.getWeekendPrice();
            default -> period.getWeekdayPrice();
        };
    }

    /**
     * 计算槽位价格并构建SlotQuote列表
     *
     * @param templates 槽位模板列表
     * @param venueId 场馆ID
     * @param venueName 场馆名称
     * @param bookingDate 预订日期
     * @param courtMap 场地ID -> 场地名称的映射
     * @return 槽位价格报价列表
     */
    @Override
    public List<SlotQuote> calculateSlotQuotes(List<VenueBookingSlotTemplate> templates,
                                               Long venueId,
                                               String venueName,
                                               LocalDate bookingDate,
                                               Map<Long, String> courtMap) {
        log.debug("开始计算槽位价格 - venueId: {}, 槽位数: {}", venueId, templates.size());

        // 批量获取所有槽位的价格
        List<LocalTime> slotStartTimes = templates.stream()
                .map(VenueBookingSlotTemplate::getStartTime)
                .distinct()
                .collect(Collectors.toList());

        Map<LocalTime, BigDecimal> priceMap = getSlotPriceMap(venueId, bookingDate, slotStartTimes);
        log.debug("批量获取价格完成 - 共{}个时间点的价格", priceMap.size());

        List<SlotQuote> slotQuotes = new ArrayList<>();

        for (VenueBookingSlotTemplate template : templates) {
            BigDecimal unitPrice = priceMap.get(template.getStartTime());

            if (unitPrice == null) {
                log.error("未找到槽位价格 - slotStartTime: {}, venueId: {}, 请检查价格模板配置",
                        template.getStartTime(), venueId);
                throw new IllegalStateException(String.format(
                        "场馆价格配置异常：未找到 %s 时段的价格，场馆ID: %d",
                        template.getStartTime(), venueId));
            }

            // 确保价格精度为分位
            unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);

            // 从courtMap获取场地名称
            String courtName = courtMap.getOrDefault(template.getCourtId(), "未知场地");

            // 计算槽位级别的额外费用
            List<ExtraQuote> slotExtras = calculateSlotExtraCharges(venueId, unitPrice);

            SlotQuote slotQuote = SlotQuote.builder()
                    .slotId(template.getBookingSlotTemplateId())
//                    .venueId(venueId)
                    .courtId(venueId)
                    .slotName(String.format("%s %s-%s", courtName, template.getStartTime(), template.getEndTime()))
                    .bookingDate(bookingDate)
                    .startTime(template.getStartTime())
                    .endTime(template.getEndTime())
                    .unitPrice(unitPrice)
                    .slotExtras(slotExtras)
                    .build();

            log.debug("槽位计价 - slotTemplateId: {}, courtId: {}, startTime: {}, endTime: {}, unitPrice: {}, 槽位费用数: {}",
                    template.getBookingSlotTemplateId(), template.getCourtId(),
                    template.getStartTime(), template.getEndTime(), unitPrice, slotExtras.size());

            slotQuotes.add(slotQuote);
        }

        return slotQuotes;
    }

    /**
     * 计算订单级别额外费用
     * 查询场馆的订单级别额外费用配置并计算金额
     *
     * @param venueId 场馆ID
     * @param basePrice 基础价格（所有槽位价格总和）
     * @param slotCount 槽位数量
     * @return 订单级别额外费用列表
     */
    @Override
    public List<OrderLevelExtraQuote> calculateExtraCharges(Long venueId, BigDecimal basePrice, int slotCount) {
        List<OrderLevelExtraQuote> extraQuotes = new ArrayList<>();

        log.debug("开始计算订单级别额外费用 - venueId: {}, basePrice: {}, slotCount: {}", venueId, basePrice, slotCount);

        // 查询场馆的已启用的订单级别额外费用配置（chargeLevel=1）
        List<VenueExtraChargeTemplate> chargeTemplates = extraChargeTemplateMapper.selectList(
                new LambdaQueryWrapper<VenueExtraChargeTemplate>()
                        .eq(VenueExtraChargeTemplate::getVenueId, venueId)
                        .eq(VenueExtraChargeTemplate::getChargeLevel, 1)  // 只查询订单级别
                        .eq(VenueExtraChargeTemplate::getIsEnabled, 1)
                        .orderByAsc(VenueExtraChargeTemplate::getChargeType)
        );
        log.debug("查询到订单级别额外费用配置 - venueId: {}, 配置数: {}", venueId, chargeTemplates.size());

        for (VenueExtraChargeTemplate template : chargeTemplates) {
            BigDecimal amount = calculateExtraChargeAmount(template, basePrice, slotCount);
            ChargeModeEnum chargeModeEnum = convertToChargeModeEnum(template.getChargeMode());

            OrderLevelExtraQuote quote = OrderLevelExtraQuote.builder()
                    .chargeTypeId(template.getTemplateId())
                    .chargeName(template.getChargeName())
                    .chargeMode(chargeModeEnum)
                    .fixedValue(template.getUnitAmount())
                    .amount(amount)
                    .build();

            extraQuotes.add(quote);

            log.debug("额外费用项 - chargeName: {}, chargeType: {}, chargeMode: {}, amount: {}",
                    template.getChargeName(), template.getChargeType(), chargeModeEnum, amount);
        }

        log.debug("订单级别额外费用计算完成 - 费用项数: {}", extraQuotes.size());
        return extraQuotes;
    }

    /**
     * 计算槽位级别额外费用
     * 查询场馆的槽位级别额外费用配置并计算金额
     *
     * @param venueId 场馆ID
     * @param slotPrice 单个槽位的价格
     * @return 槽位级别额外费用列表
     */
    public List<ExtraQuote> calculateSlotExtraCharges(Long venueId, BigDecimal slotPrice) {
        List<ExtraQuote> extraQuotes = new ArrayList<>();

        log.debug("开始计算槽位级别额外费用 - venueId: {}, slotPrice: {}", venueId, slotPrice);

        // 查询场馆的已启用的槽位级别额外费用配置（chargeLevel=2）
        List<VenueExtraChargeTemplate> chargeTemplates = extraChargeTemplateMapper.selectList(
                new LambdaQueryWrapper<VenueExtraChargeTemplate>()
                        .eq(VenueExtraChargeTemplate::getVenueId, venueId)
                        .eq(VenueExtraChargeTemplate::getChargeLevel, 2)  // 只查询槽位级别
                        .eq(VenueExtraChargeTemplate::getIsEnabled, 1)
                        .orderByAsc(VenueExtraChargeTemplate::getChargeType)
        );
        log.debug("查询到槽位级别额外费用配置 - venueId: {}, 配置数: {}", venueId, chargeTemplates.size());

        for (VenueExtraChargeTemplate template : chargeTemplates) {
            BigDecimal amount = calculateExtraChargeAmount(template, slotPrice, 1);  // slotCount=1，表示单个槽位
            ChargeModeEnum chargeModeEnum = convertToChargeModeEnum(template.getChargeMode());

            ExtraQuote quote = ExtraQuote.builder()
                    .chargeTypeId(template.getTemplateId())
                    .chargeName(template.getChargeName())
                    .chargeMode(chargeModeEnum)
                    .fixedValue(template.getUnitAmount())
                    .amount(amount)
                    .build();

            extraQuotes.add(quote);

            log.debug("槽位费用项 - chargeName: {}, chargeType: {}, chargeMode: {}, amount: {}",
                    template.getChargeName(), template.getChargeType(), chargeModeEnum, amount);
        }

        log.debug("槽位级别额外费用计算完成 - 费用项数: {}", extraQuotes.size());
        return extraQuotes;
    }

    /**
     * 计算单个额外费用金额
     *
     * @param template 额外费用模板
     * @param basePrice 基础价格（所有槽位价格总和）
     * @param slotCount 槽位数量
     * @return 计算后的金额
     */
    private BigDecimal calculateExtraChargeAmount(VenueExtraChargeTemplate template,
                                                   BigDecimal basePrice,
                                                   Integer slotCount) {
        if (template == null || template.getChargeMode() == null) {
            return BigDecimal.ZERO;
        }

        Integer chargeLevel = template.getChargeLevel();
        Integer chargeMode = template.getChargeMode();
        BigDecimal unitAmount = template.getUnitAmount();

        if (unitAmount == null) {
            return BigDecimal.ZERO;
        }

        // 百分比计费 - 订单级别和订单项级别计算方式相同
        if (ChargeModeEnum.PERCENTAGE.getCode().equals(chargeMode)) {
            return basePrice.multiply(unitAmount).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        }

        // FIXED 固定金额 - 根据费用级别计算
        if (ChargeModeEnum.FIXED.getCode().equals(chargeMode)) {
            // 订单级别：固定金额，不乘以槽位数
            if (chargeLevel == 1) {  // ORDER_LEVEL
                return unitAmount.setScale(2, RoundingMode.HALF_UP);
            }
            // 订单项级别：固定金额乘以槽位数
            else if (chargeLevel == 2) {  // ORDER_ITEM_LEVEL
                if (slotCount != null && slotCount > 0) {
                    return unitAmount.multiply(new BigDecimal(slotCount)).setScale(2, RoundingMode.HALF_UP);
                }
            }
        }

        return BigDecimal.ZERO;
    }

    /**
     * 将额外费用表的计费模式转换为ChargeModeEnum
     *
     * @param chargeMode 额外费用表的计费模式 (1=FIXED, 2=PERCENTAGE)
     * @return 对应的ChargeModeEnum
     */
    private ChargeModeEnum convertToChargeModeEnum(Integer chargeMode) {
        if (chargeMode == null) {
            return ChargeModeEnum.FIXED;
        }

        return switch (chargeMode) {
            case 1 -> ChargeModeEnum.FIXED;        // 固定金额
            case 2 -> ChargeModeEnum.PERCENTAGE;   // 按百分比计费
            default -> ChargeModeEnum.FIXED;
        };
    }
}
