package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.enums.order.ChargeModeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.VenueCode;
import com.unlimited.sports.globox.dubbo.merchant.dto.OrderLevelExtraQuote;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.venue.dto.DetailedPricingInfo;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.venue.entity.booking.VenuePriceOverride;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueExtraChargeTemplate;
import com.unlimited.sports.globox.venue.adapter.dto.AwaySlotPrice;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplate;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplatePeriod;
import com.unlimited.sports.globox.model.venue.enums.DayType;
import com.unlimited.sports.globox.venue.mapper.VenueExtraChargeTemplateMapper;
import com.unlimited.sports.globox.venue.mapper.VenuePriceOverrideMapper;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplateMapper;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplatePeriodMapper;
import com.unlimited.sports.globox.venue.service.IVenuePriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 新版价格服务实现 - V2版本
 * 支持按场地（Court）分别配置价格，而不是整个场馆统一价格
 *
 * 核心改进：
 * 1. 每个场地（Court）可以有独立的价格模板（priceTemplateId）
 * 2. 如果场地没有单独配置，则使用场馆默认价格模板
 * 3. 返回详细的按场地分组的价格信息
 * 4. 为向后兼容性，提供聚合的时间->价格映射
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
     * 将Away槽位价格列表转换为DetailedPricingInfo格式
     * 用于统一Away和Home的价格返回格式
     *
     * @param awaySlotPrices Away槽位价格列表（来自第三方平台）
     * @param templates 用户选中的槽位模板列表（用于计算总价）
     * @param courtMap 场地映射表
     * @return 详细价格信息
     */
    @Override
    public DetailedPricingInfo convertAwaySlotPricesToDetailedPricingInfo(
            List<AwaySlotPrice> awaySlotPrices,
            List<VenueBookingSlotTemplate> templates,
            Map<Long, Court> courtMap) {
        log.info("[价格转换] Away槽位价格转换为DetailedPricingInfo - 总槽位数: {}, 选中槽位数: {}", awaySlotPrices.size(), templates.size());

        // 构建所有可用槽位的价格映射
        Map<Long, Map<LocalTime, BigDecimal>> pricesByCourtId = awaySlotPrices.stream()
                .map(slotPrice -> {
                    // 找到对应的courtId
                    Court court = courtMap.values().stream()
                            .filter(c -> c.getThirdPartyCourtId().equals(slotPrice.getThirdPartyCourtId()))
                            .findFirst()
                            .orElse(null);

                    if (court == null) {
                        log.warn("[价格转换] 未找到对应的本地场地 - thirdPartyCourtId: {}", slotPrice.getThirdPartyCourtId());
                        return null;
                    }

                    return Map.entry(court.getCourtId(), Map.entry(slotPrice.getStartTime(), slotPrice.getPrice()));
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.toMap(
                                e -> e.getValue().getKey(),
                                e -> e.getValue().getValue()
                        )
                ));

        Map<Long, Long> courtIdToTemplateIdMap = new HashMap<>();

        // 计算基础价格：只计算用户选中的模板对应的槽位价格
        BigDecimal basePrice = templates.stream()
                .map(template -> {
                    Map<LocalTime, BigDecimal> courtPrices = pricesByCourtId.getOrDefault(template.getCourtId(), Collections.emptyMap());
                    return courtPrices.getOrDefault(template.getStartTime(), BigDecimal.ZERO);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("[价格转换] 转换完成 - 场地数: {}, 选中槽位总价: {}", pricesByCourtId.size(), basePrice);

        // todo away考虑额外费用的问题
        return DetailedPricingInfo.builder()
                .pricesByCourtId(pricesByCourtId)
                .basePrice(basePrice)
                .orderLevelExtras(Collections.emptyList())
                .orderLevelExtraAmount(BigDecimal.ZERO)
                .itemLevelExtrasByCourtId(new HashMap<>())
                .totalPrice(basePrice)
                .courtIdToTemplateIdMap(courtIdToTemplateIdMap)
                .build();
    }

    /**
     * 计算完整价格 - 按场地分组
     * 流程：
     * 1. 按场地分组templates
     * 2. 对每个场地，获取其专属价格模板（每个Court必须配置priceTemplateId）
     * 3. 为每个(courtId, startTime)对查询价格
     * 4. 计算额外费用（订单级和项级）
     * 5. 返回详细的价格信息
     *
     * @param templates 槽位模板列表
     * @param venueId 场馆ID
     * @param bookingDate 预订日期
     * @param courtMap 场地映射表（courtId -> Court）
     * @return 详细价格信息
     */
    @Override
    public DetailedPricingInfo calculatePricingByCourtTemplates(
            List<VenueBookingSlotTemplate> templates,
            Long venueId,
            LocalDate bookingDate,
            Map<Long, Court> courtMap) {
        log.info("[价格计算] 开始计算 - venueId: {}, 槽位数: {}, 日期: {}",
                venueId, templates.size(), bookingDate);
        //按场地分组templates
        Map<Long, List<VenueBookingSlotTemplate>> templatesByCourtId = templates.stream()
                .collect(Collectors.groupingBy(VenueBookingSlotTemplate::getCourtId));
        log.info("[价格计算] 按场地分组 - 场地数: {}", templatesByCourtId.size());
        // 为每个场地确定有效的价格模板ID
        Map<Long, Long> courtIdToTemplateIdMap = new HashMap<>();
        Set<Long> templateIds = new HashSet<>();
        templatesByCourtId.forEach((courtId, templateList) -> {
            Court court = courtMap.get(courtId);
            if (court == null) {
                log.warn("[价格计算V2] 场地不存在 - courtId: {}", courtId);
                return; // Stream中的return相当于for循环的continue
            }

            Long priceTemplateId = court.getTemplateId();
            if (priceTemplateId == null) {
                log.warn("[价格计算V2] 场地未配置价格模板 - courtId: {}, 场地名: {}", courtId, court.getName());
                courtIdToTemplateIdMap.put(courtId, null);
            } else {
                courtIdToTemplateIdMap.put(courtId, priceTemplateId);
                templateIds.add(priceTemplateId);
            }
        });
        log.info("[价格计算V2] 需要查询的模板ID数: {}", templateIds.size());
        // 批量查询所有价格模板
        if(ObjectUtils.isEmpty(templateIds)) {
            throw new GloboxApplicationException(VenueCode.VENUE_PRICE_NOT_CONFIGURED);
        }
        List<VenuePriceTemplate> allTemplates = priceTemplateMapper.selectBatchIds(new ArrayList<>(templateIds));
        Map<Long, VenuePriceTemplate> templateMap = allTemplates.stream()
                .filter(VenuePriceTemplate::getIsEnabled)
                .collect(Collectors.toMap(VenuePriceTemplate::getTemplateId, Function.identity()));
        log.info("[价格计算] 批量查询价格模板完成 - 查询数: {}, 有效数: {}", templateIds.size(), templateMap.size());
        // 批量查询所有价格时段
        List<VenuePriceTemplatePeriod> allPeriods = priceTemplatePeriodMapper.selectList(
                new LambdaQueryWrapper<VenuePriceTemplatePeriod>()
                        .in(VenuePriceTemplatePeriod::getTemplateId, templateIds)
                        .eq(VenuePriceTemplatePeriod::getIsEnabled, true)
                        .orderByAsc(VenuePriceTemplatePeriod::getStartTime)
        );
        Map<Long, List<VenuePriceTemplatePeriod>> periodsByTemplateId = allPeriods.stream()
                .collect(Collectors.groupingBy(VenuePriceTemplatePeriod::getTemplateId));
        log.info("[价格计算] 批量查询价格时段完成 - 总数: {}", allPeriods.size());
        // 批量查询所有价格覆盖（所有场地共享）
        List<VenuePriceOverride> overrides = priceOverrideMapper.selectList(
                new LambdaQueryWrapper<VenuePriceOverride>()
                        .eq(VenuePriceOverride::getVenueId, venueId)
                        .eq(VenuePriceOverride::getOverrideDate, bookingDate)
                        .eq(VenuePriceOverride::getIsEnabled, 1)
        );
        log.info("[价格计算V2] 批量查询价格覆盖完成 - 覆盖数: {}", overrides.size());
        //  为每个场地计算价格
        DayType dayType = determineDayType(bookingDate);
        Map<Long, Map<LocalTime, BigDecimal>> pricesByCourtId = templatesByCourtId.entrySet().stream()
                .map(entry -> {
                    Long courtId = entry.getKey();
                    Long templateId = courtIdToTemplateIdMap.get(courtId);
                    //  基础校验：模板 ID 是否存在
                    if (templateId == null) return null;
                    //  模板对象校验
                    VenuePriceTemplate template = templateMap.get(templateId);
                    if (template == null) {
                        log.warn("[价格计算] 价格模板不存在或未启用 - courtId: {}, templateId: {}", courtId, templateId);
                        return null;
                    }
                    // 时段配置校验
                    List<VenuePriceTemplatePeriod> periods = periodsByTemplateId.getOrDefault(templateId, Collections.emptyList());
                    if (periods.isEmpty()) {
                        log.warn("[价格计算]价格模板未配置时段 - courtId: {}, templateId: {}", courtId, templateId);
                        return null;
                    }
                    // 执行计算逻辑
                    Map<LocalTime, BigDecimal> courtPrices = calculateCourtPriceMap(
                            courtId, entry.getValue(), overrides, periods, dayType, bookingDate);
                    log.info("[价格计算] 场地价格计算完成 - courtId: {}, 模板ID: {}, 找到价格数: {}",
                            courtId, templateId, courtPrices.size());
                    return Map.entry(courtId, courtPrices);
                })
                .filter(Objects::nonNull) // 过滤掉因校验失败返回的空值
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // 计算基础价格总和
        BigDecimal basePrice = pricesByCourtId.values().stream()
                .flatMap(m -> m.values().stream())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("[价格计算V2] 基础价格计算完成 - 基础价格: {}", basePrice);

        //  批量查询所有额外费用模板（订单级和订单项级）
        List<VenueExtraChargeTemplate> allExtraCharges = extraChargeTemplateMapper.selectList(
                new LambdaQueryWrapper<VenueExtraChargeTemplate>()
                        .eq(VenueExtraChargeTemplate::getVenueId, venueId)
                        .eq(VenueExtraChargeTemplate::getIsEnabled, 1)
                        .orderByAsc(VenueExtraChargeTemplate::getChargeType)
        );

        // 按chargeLevel分组：1=订单级，2=订单项级
        List<VenueExtraChargeTemplate> orderLevelCharges = allExtraCharges.stream()
                .filter(t -> t.getChargeLevel() == 1)
                .collect(Collectors.toList());
        List<VenueExtraChargeTemplate> itemLevelCharges = allExtraCharges.stream()
                .filter(t -> t.getChargeLevel() == 2)
                .collect(Collectors.toList());

        log.info("[价格计算] 批量查询额外费用模板完成 - 订单级: {}, 订单项级: {}",
                orderLevelCharges.size(), itemLevelCharges.size());

        // 计算订单级额外费用
        List<OrderLevelExtraQuote> orderLevelExtrasQuotes = calculateExtraCharges(
                orderLevelCharges,
                basePrice);

        List<DetailedPricingInfo.OrderLevelExtraInfo> orderLevelExtras = orderLevelExtrasQuotes.stream()
                .map(extra -> DetailedPricingInfo.OrderLevelExtraInfo.builder()
                        .chargeTypeId(extra.getChargeTypeId())
                        .chargeName(extra.getChargeName())
                        .chargeMode(extra.getChargeMode().getCode())
                        .fixedValue(extra.getFixedValue())
                        .amount(extra.getAmount())
                        .build())
                .collect(Collectors.toList());

        BigDecimal orderLevelExtraAmount = orderLevelExtras.stream()
                .map(DetailedPricingInfo.OrderLevelExtraInfo::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 计算项级额外费用
        Map<Long, List<DetailedPricingInfo.ItemLevelExtraInfo>> itemLevelExtrasByCourtId =
                calculateItemLevelExtrasByCourtId(templates, itemLevelCharges, pricesByCourtId);

        // 计算所有项级额外费用的总和
        BigDecimal itemLevelExtraAmount = itemLevelExtrasByCourtId.entrySet().stream()
                .map(entry -> {
                    Long courtId = entry.getKey();
                    List<DetailedPricingInfo.ItemLevelExtraInfo> extras = entry.getValue();
                    int courtSlotCount = templatesByCourtId.getOrDefault(courtId, Collections.emptyList()).size();

                    BigDecimal courtExtraAmount = extras.stream()
                            .map(DetailedPricingInfo.ItemLevelExtraInfo::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .multiply(new BigDecimal(courtSlotCount));

                    return courtExtraAmount;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 6. 计算总价
        BigDecimal totalPrice = basePrice.add(orderLevelExtraAmount).add(itemLevelExtraAmount);

        log.info("[价格计算V2] 价格计算完成 - 基础价格: {}, 订单级额外: {}, 项级额外: {}, 总价: {}",
                basePrice, orderLevelExtraAmount, itemLevelExtraAmount, totalPrice);

        // 7. 返回结果
        return DetailedPricingInfo.builder()
                .pricesByCourtId(pricesByCourtId)
                .basePrice(basePrice)
                .orderLevelExtras(orderLevelExtras)
                .orderLevelExtraAmount(orderLevelExtraAmount)
                .itemLevelExtrasByCourtId(itemLevelExtrasByCourtId)
                .totalPrice(totalPrice)
                .courtIdToTemplateIdMap(courtIdToTemplateIdMap)
                .build();
    }

    /**
     * 为单个场地计算价格映射（内存操作，使用预加载的数据）
     *
     * @param courtId 场地ID
     * @param templates 该场地的所有槽位模板
     * @param overrides 价格覆盖列表（已预加载）
     * @param periods 价格时段列表（已预加载）
     * @param dayType 日期类型
     * @param bookingDate 预订日期
     * @return 时间 -> 价格映射
     */
    private Map<LocalTime, BigDecimal> calculateCourtPriceMap(
            Long courtId,
            List<VenueBookingSlotTemplate> templates,
            List<VenuePriceOverride> overrides,
            List<VenuePriceTemplatePeriod> periods,
            DayType dayType,
            LocalDate bookingDate) {

        // 提取所有不同的开始时间
        List<LocalTime> slotTimes = templates.stream()
                .map(VenueBookingSlotTemplate::getStartTime)
                .distinct()
                .toList();

        // 构建startTime -> template的映射，用于获取对应的endTime
        Map<LocalTime, VenueBookingSlotTemplate> timeToTemplateMap = templates.stream()
                .collect(Collectors.toMap(VenueBookingSlotTemplate::getStartTime, t -> t, (t1, t2) -> t1));

        return slotTimes.stream()
                .map(slotTime -> {
                    VenueBookingSlotTemplate template = timeToTemplateMap.get(slotTime);
                    LocalTime endTime = template.getEndTime();
                    // 查找价格覆盖
                    BigDecimal price = overrides.stream()
                            .filter(override -> override.isValidForTimeRange(bookingDate, slotTime, endTime))
                            .findFirst()
                            .map(VenuePriceOverride::getOverridePrice)
                            .orElse(null);

                    // 如果没有覆盖价格，从模板中获取
                    if (price == null) {
                        price = getTemplatePriceBySlotTime(slotTime, periods, dayType);
                    }

                    if (price != null) {
                        log.debug("[价格确认] courtId: {}, 时间: {}-{}, 价格: {}",
                                courtId, slotTime, endTime, price);
                        return Map.entry(slotTime, price);
                    } else {
                        log.error("[价格缺失] 未找到价格 - courtId: {}, 时间: {}-{}",
                                courtId, slotTime, endTime);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }



    /**
     * 判断日期类型
     */
    private DayType determineDayType(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return DayType.WEEKEND;
        }

        // TODO: 检查是否是节假日
        // 暂时返回工作日
        return DayType.WEEKDAY;
    }

    /**
     * 根据时间从模板时段中查找价格
     */
    private BigDecimal getTemplatePriceBySlotTime(
            LocalTime slotTime,
            List<VenuePriceTemplatePeriod> periods,
            DayType dayType) {

        for (VenuePriceTemplatePeriod period : periods) {
            if (!slotTime.isBefore(period.getStartTime()) && slotTime.isBefore(period.getEndTime())) {
                // 在这个时段内，根据日期类型返回对应价格
                BigDecimal price = switch (dayType) {
                    case WEEKDAY -> period.getWeekdayPrice();
                    case WEEKEND -> period.getWeekendPrice();
                    case HOLIDAY -> period.getHolidayPrice() != null ? period.getHolidayPrice() : period.getWeekdayPrice();
                };
                return price;
            }
        }
        return null;
    }

    /**
     * 计算订单级额外费用
     *
     * @param chargeTemplates
     * @param basePrice 基础价格
     * @return 订单级额外费用列表
     */
    private List<OrderLevelExtraQuote> calculateExtraCharges(
            List<VenueExtraChargeTemplate> chargeTemplates,
            BigDecimal basePrice) {
        log.debug("[额外费用] 订单级计算 - 配置数: {}",
                Optional.ofNullable(chargeTemplates).map(List::size).orElse(0));
        if (chargeTemplates == null || chargeTemplates.isEmpty()) {
            return Collections.emptyList();
        }
        return chargeTemplates.stream()
                .map(template -> {
                    BigDecimal amount = calculateExtraChargeAmount(template, basePrice);
                    ChargeModeEnum chargeModeEnum = ChargeModeEnum.getByCode(template.getChargeMode());
                    log.debug("[额外费用] 订单级 - chargeName: {}, chargeMode: {}, amount: {}",
                            template.getChargeName(), chargeModeEnum, amount);
                    return OrderLevelExtraQuote.builder()
                            .chargeTypeId(template.getTemplateId())
                            .chargeName(template.getChargeName())
                            .chargeMode(chargeModeEnum)
                            .fixedValue(template.getUnitAmount())
                            .amount(amount)
                            .build();
                })
                .collect(Collectors.toList());
    }
    /**
     * 计算额外费用金额
     * 逻辑：
     * 1. 百分比计费：额外费用 = 基础价格 × 百分比 / 100
     * 2. 固定金额计费：
     *    - 订单级：返回固定金额（不受槽位数影响）
     *    - 订单项级：返回单位金额（每个槽位的费用）
     */
    private BigDecimal calculateExtraChargeAmount(
            VenueExtraChargeTemplate template,
            BigDecimal basePrice) {
        if (template == null || template.getChargeMode() == null) {
            return BigDecimal.ZERO;
        }
        Integer chargeMode = template.getChargeMode();
        BigDecimal unitAmount = template.getUnitAmount();
        if (unitAmount == null) {
            return BigDecimal.ZERO;
        }
        // 百分比计费
        if (ChargeModeEnum.PERCENTAGE.getCode().equals(chargeMode)) {
            return basePrice.multiply(unitAmount).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        }
        // 固定金额计费
        if (ChargeModeEnum.FIXED.getCode().equals(chargeMode)) {
            return unitAmount.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 计算项级额外费用（按场地分组）
     *
     * @param templates 槽位模板列表
     * @param itemLevelCharges 订单项级别额外费用模板列表
     * @param pricesByCourtId 按场地分组的价格映射
     * @return 按courtId分组的项级额外费用
     */
    private Map<Long, List<DetailedPricingInfo.ItemLevelExtraInfo>> calculateItemLevelExtrasByCourtId(
            List<VenueBookingSlotTemplate> templates,
            List<VenueExtraChargeTemplate> itemLevelCharges,
            Map<Long, Map<LocalTime, BigDecimal>> pricesByCourtId) {
        log.debug("[额外费用] 项级计算 - 配置数: {}", itemLevelCharges.size());
        if (templates == null || templates.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<VenueBookingSlotTemplate>> courtToSlotTemplateMap = templates.stream().collect(Collectors.groupingBy(VenueBookingSlotTemplate::getCourtId));
        //先按 courtId 分组
        return courtToSlotTemplateMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, // courtId
                        entry -> {
                            Long courtId = entry.getKey();
                            List<VenueBookingSlotTemplate> courtTemplates = entry.getValue();
                            //  计算该场地的基础总价
                            BigDecimal courtBasePrice = courtTemplates.stream()
                                    .map(slot -> pricesByCourtId.getOrDefault(courtId, Collections.emptyMap())
                                            .getOrDefault(slot.getStartTime(), BigDecimal.ZERO))
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                            // 过滤并转换适用的额外费用
                            return itemLevelCharges.stream()
                                    .filter(charge -> isChargeApplicable(charge, courtId))
                                    .map(charge -> {
                                        BigDecimal amount = calculateExtraChargeAmount(charge, courtBasePrice);
                                        return DetailedPricingInfo.ItemLevelExtraInfo.builder()
                                                .chargeTypeId(charge.getTemplateId())
                                                .chargeName(charge.getChargeName())
                                                .chargeMode(charge.getChargeMode())
                                                .fixedValue(charge.getUnitAmount())
                                                .amount(amount)
                                                .perSlotAmount(amount)
                                                .build();
                                    })
                                    .collect(Collectors.toList());
                        }
                ));
    }

    /**
     * 抽取判断逻辑
     */
    private boolean isChargeApplicable(VenueExtraChargeTemplate charge, Long courtId) {
        List<Long> applicableIds = charge.getApplicableCourtIds();
        return applicableIds == null || applicableIds.isEmpty() || applicableIds.contains(courtId);
    }
}
