package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.enums.order.ChargeModeEnum;
import com.unlimited.sports.globox.dubbo.merchant.dto.OrderLevelExtraQuote;
import com.unlimited.sports.globox.venue.dto.PricingInfo;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplate;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplatePeriod;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueExtraChargeTemplate;
import com.unlimited.sports.globox.model.venue.enums.DayType;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.venue.entity.booking.VenuePriceOverride;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplatePeriodMapper;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplateMapper;
import com.unlimited.sports.globox.venue.mapper.VenueExtraChargeTemplateMapper;
import com.unlimited.sports.globox.venue.mapper.VenuePriceOverrideMapper;
import com.unlimited.sports.globox.venue.service.IVenuePriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 场馆价格服务实现
 * 统一处理价格相关的所有查询和计算
 */
@Slf4j
@Service
public class VenuePriceServiceImpl implements IVenuePriceService {

    @Autowired
    private VenuePriceTemplateMapper priceTemplateMapper;

    @Autowired
    private VenuePriceTemplatePeriodMapper priceTemplatePeriodMapper;

    @Autowired
    private VenueExtraChargeTemplateMapper extraChargeTemplateMapper;

    @Autowired
    private VenuePriceOverrideMapper priceOverrideMapper;


    /**
     * 批量查询多个时间点的价格
     * 通过构建内部缓存，避免重复查询，包含价格覆盖处理
     *
     * @param venuePriceTemplateId 场馆价格模版ID
     * @param venueId 场馆ID（用于查询价格覆盖）
     * @param bookingDate 预订日期
     * @param slotTimes 槽位时间列表（开始时间）
     * @return 时间 -> 价格 的映射
     */
    @Override
    public Map<LocalTime, BigDecimal> getSlotPriceMap(Long venuePriceTemplateId, Long venueId, LocalDate bookingDate, List<LocalTime> slotTimes) {
        Map<LocalTime, BigDecimal> priceMap = new HashMap<>();

        if (slotTimes == null || slotTimes.isEmpty()) {
            return priceMap;
        }

        // 获取价格模板
        VenuePriceTemplate template = priceTemplateMapper.selectById(venuePriceTemplateId);
        if (template == null || !template.getIsEnabled()) {
            log.warn("价格模板不存在或未启用: templateId={}", venuePriceTemplateId);
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

        // 查询该日期该场馆的所有启用的价格覆盖
        List<VenuePriceOverride> overrides = priceOverrideMapper.selectList(
                new LambdaQueryWrapper<VenuePriceOverride>()
                        .eq(VenuePriceOverride::getVenueId, venueId)
                        .eq(VenuePriceOverride::getOverrideDate, bookingDate)
                        .eq(VenuePriceOverride::getIsEnabled, 1)
        );

        log.debug("查询到价格覆盖记录 - venueId: {}, overrideDate: {}, 数量: {}", venueId, bookingDate, overrides.size());

        // 预构建override价格映射表（时间 -> 覆盖价格），只存储有覆盖的时间
        Map<LocalTime, BigDecimal> overridePriceMap = slotTimes.stream()
                .collect(HashMap::new,
                        (map, slotTime) -> {
                            overrides.stream()
                                    .filter(override -> override.isValidForTimeRange(bookingDate, slotTime, slotTime))
                                    .findFirst()
                                    .ifPresent(override -> {
                                        map.put(slotTime, override.getOverridePrice());
                                        log.debug("应用价格覆盖 - time={}, overridePrice={}", slotTime, override.getOverridePrice());
                                    });
                        },
                        HashMap::putAll);

        // 确定日期类型
        DayType dayType = determineDayType(bookingDate);

        // 为每个槽位时间查找价格（先查override，再查template）
        slotTimes.forEach(slotTime -> {
            BigDecimal price = overridePriceMap.getOrDefault(slotTime, null);
            if (price == null) {
                price = getTemplatePriceBySlotTime(slotTime, periods, dayType);
            }
            if (price != null) {
                priceMap.put(slotTime, price);
            }
        });

        log.debug("价格查询完成: 共{}个时间，找到{}个价格", slotTimes.size(), priceMap.size());
        return priceMap;
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
     * 从模板中获取槽位价格
     *
     * @param slotTime 槽位时间
     * @param periods 价格时段列表
     * @param dayType 日期类型
     * @return 模板价格（可能为null）
     */
    private BigDecimal getTemplatePriceBySlotTime(LocalTime slotTime,
            List<VenuePriceTemplatePeriod> periods, DayType dayType) {
        for (VenuePriceTemplatePeriod period : periods) {
            if (isTimeInPeriod(slotTime, period.getStartTime(), period.getEndTime())) {
                BigDecimal price = selectPriceByDayType(period, dayType);
                if (price != null) {
                    log.debug("找到模板价格: time={}, price={}", slotTime, price);
                }
                return price;
            }
        }
        return null;
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
            // 订单项级别：返回单位金额（每个槽位的费用），不乘以槽位数
            else if (chargeLevel == 2) {  // ORDER_ITEM_LEVEL
                // amount 代表单个槽位的费用，使用时需要根据实际槽位数计算总金额
                return unitAmount.setScale(2, RoundingMode.HALF_UP);
            }
        }

        return BigDecimal.ZERO;
    }



    /**
     * 计算完整价格（槽位价格 + 额外费用）- 使用槽位模板
     * 用于预览场景，不需要实际占用槽位
     *
     * @param templates 槽位模板列表
     * @param venuePriceTemplateId 场馆价格配置ID
     * @param bookingDate 预订日期
     * @return 完整价格计算结果
     */
    @Override
    public PricingInfo calculateCompletePricingByTemplates(
            List<VenueBookingSlotTemplate> templates,
            Long venueId,
            Long venuePriceTemplateId,
            LocalDate bookingDate) {

        // 提取所有不同的槽位开始时间
        List<LocalTime> slotStartTimes = templates.stream()
                .map(VenueBookingSlotTemplate::getStartTime)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询价格（包含价格覆盖）
        Map<LocalTime, BigDecimal> priceMap = getSlotPriceMap(venuePriceTemplateId, venueId, bookingDate, slotStartTimes);

        // 计算基础价格总和
        BigDecimal basePrice = templates.stream()
                .map(template -> priceMap.getOrDefault(template.getStartTime(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算订单级额外费用
        List<OrderLevelExtraQuote> orderLevelExtrasQuotes = calculateExtraCharges(
                venueId,
                basePrice,
                templates.size()
        );

        // 转换额外费用格式
        List<PricingInfo.OrderLevelExtraInfo> orderLevelExtras = orderLevelExtrasQuotes.stream()
                .map(extra -> PricingInfo.OrderLevelExtraInfo.builder()
                        .chargeTypeId(extra.getChargeTypeId())
                        .chargeName(extra.getChargeName())
                        .chargeMode(extra.getChargeMode().getCode())
                        .fixedValue(extra.getFixedValue())
                        .amount(extra.getAmount())
                        .build())
                .collect(Collectors.toList());

        // 计算订单级额外费用总和
        BigDecimal orderLevelExtraAmount = orderLevelExtras.stream()
                .map(PricingInfo.OrderLevelExtraInfo::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算订单项级额外费用（按场地分组）
        Map<Long, List<PricingInfo.ItemLevelExtraInfo>> itemLevelExtrasByCourtId =
                calculateItemLevelExtrasByCourtId(templates, venueId, priceMap);

        // 按场地分组templates，用于计算每个场地的槽位数
        Map<Long, List<VenueBookingSlotTemplate>> templatesByCourtId = templates.stream()
                .collect(Collectors.groupingBy(VenueBookingSlotTemplate::getCourtId));

        // 计算所有项级额外费用的总和（单位金额 × 对应场地的槽位数）
        BigDecimal itemLevelExtraAmount = itemLevelExtrasByCourtId.entrySet().stream()
                .map(entry -> {
                    Long courtId = entry.getKey();
                    List<PricingInfo.ItemLevelExtraInfo> extras = entry.getValue();
                    int courtSlotCount = templatesByCourtId.getOrDefault(courtId, Collections.emptyList()).size();

                    // 该场地的总额外费用 = 单位额外费用之和 × 槽位数
                    BigDecimal courtExtraAmount = extras.stream()
                            .map(PricingInfo.ItemLevelExtraInfo::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .multiply(new BigDecimal(courtSlotCount));

                    return courtExtraAmount;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 总价格 = 基础价格 + 订单级额外费用 + 项级额外费用
        BigDecimal totalPrice = basePrice.add(orderLevelExtraAmount).add(itemLevelExtraAmount);

        // 返回价格信息
        return PricingInfo.builder()
                .slotPrices(priceMap)
                .basePrice(basePrice)
                .orderLevelExtras(orderLevelExtras)
                .orderLevelExtraAmount(orderLevelExtraAmount)
                .itemLevelExtrasByCourtId(itemLevelExtrasByCourtId)
                .totalPrice(totalPrice)
                .build();
    }



    /**
     * 计算订单级别额外费用
     * 查询场馆的订单级别额外费用配置并计算金额
     *
     * @param venueId 场馆价格配置ID
     * @param basePrice 基础价格（所有槽位价格总和）
     * @param slotCount 槽位数量
     * @return 订单级别额外费用列表
     */
    public List<OrderLevelExtraQuote> calculateExtraCharges(Long venueId, BigDecimal basePrice, int slotCount) {
        List<OrderLevelExtraQuote> extraQuotes = new ArrayList<>();


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
            ChargeModeEnum chargeModeEnum = ChargeModeEnum.getByCode(template.getChargeMode());

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
     * 计算订单项级额外费用（按场地分组）
     * 根据模板中的courtId分组，计算每个场地的订单项级额外费用
     * 订单项级额外费用是基于该场地的槽位数量来计算的
     *
     * @param templates 槽位模板列表
     * @param venueId 场馆ID
     * @param priceMap 槽位价格映射
     * @return 按courtId分组的订单项级额外费用
     */
    private Map<Long, List<PricingInfo.ItemLevelExtraInfo>> calculateItemLevelExtrasByCourtId(
            List<VenueBookingSlotTemplate> templates,
            Long venueId,
            Map<LocalTime, BigDecimal> priceMap) {

        // 按courtId分组
        Map<Long, List<VenueBookingSlotTemplate>> templatesByCourtId = templates.stream()
                .collect(Collectors.groupingBy(VenueBookingSlotTemplate::getCourtId));

        // 查询场馆的订单项级别额外费用配置
        List<VenueExtraChargeTemplate> itemLevelCharges = extraChargeTemplateMapper.selectList(
                new LambdaQueryWrapper<VenueExtraChargeTemplate>()
                        .eq(VenueExtraChargeTemplate::getVenueId, venueId)
                        .eq(VenueExtraChargeTemplate::getChargeLevel, 2)  // 订单项级别
                        .eq(VenueExtraChargeTemplate::getIsEnabled, 1)
                        .orderByAsc(VenueExtraChargeTemplate::getChargeType)
        );

        log.debug("查询到订单项级别额外费用配置 - venueId: {}, 配置数: {}", venueId, itemLevelCharges.size());

        Map<Long, List<PricingInfo.ItemLevelExtraInfo>> result = new HashMap<>();

        // 为每个场地计算订单项级额外费用
        for (Map.Entry<Long, List<VenueBookingSlotTemplate>> entry : templatesByCourtId.entrySet()) {
            Long courtId = entry.getKey();
            List<VenueBookingSlotTemplate> courtTemplates = entry.getValue();
            int slotCount = courtTemplates.size();

            // 计算该场地的基础价格（所有槽位价格总和）
            BigDecimal courtBasePrice = courtTemplates.stream()
                    .map(template -> priceMap.getOrDefault(template.getStartTime(), BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<PricingInfo.ItemLevelExtraInfo> itemExtras = new ArrayList<>();

            // 对于该场地的每个适用的额外费用配置
            for (VenueExtraChargeTemplate chargeTemplate : itemLevelCharges) {
                // 检查是否适用于该场地（为空或包含该场地ID）
                if (chargeTemplate.getApplicableCourtIds() != null &&
                        !chargeTemplate.getApplicableCourtIds().isEmpty() &&
                        !chargeTemplate.getApplicableCourtIds().contains(courtId)) {
                    continue;  // 不适用于该场地
                }

                // 计算该费用的金额（返回单个槽位的费用）
                BigDecimal amount = calculateExtraChargeAmount(chargeTemplate, courtBasePrice, slotCount);

                PricingInfo.ItemLevelExtraInfo itemExtra = PricingInfo.ItemLevelExtraInfo.builder()
                        .chargeTypeId(chargeTemplate.getTemplateId())
                        .chargeName(chargeTemplate.getChargeName())
                        .chargeMode(chargeTemplate.getChargeMode())
                        .fixedValue(chargeTemplate.getUnitAmount())
                        .amount(amount)
                        .perSlotAmount(amount)  // perSlotAmount 和 amount 相同
                        .build();

                itemExtras.add(itemExtra);

                log.debug("订单项费用 - courtId: {}, slotCount: {}, chargeName: {}, perSlotAmount: {}, totalAmount: {}",
                        courtId, slotCount, chargeTemplate.getChargeName(), amount, amount.multiply(new BigDecimal(slotCount)));
            }

            result.put(courtId, itemExtras);
        }

        return result;
    }
}
