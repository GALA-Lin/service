package com.unlimited.sports.globox.venue.service;

import com.unlimited.sports.globox.dubbo.merchant.dto.OrderLevelExtraQuote;
import com.unlimited.sports.globox.venue.dto.PricingInfo;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.venue.enums.DayType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 场馆价格服务
 * 专门处理价格查询和计算
 */
public interface IVenuePriceService {

    /**
     * 批量查询多个时间点的价格
     * 用于场馆搜索和槽位查询，包含价格覆盖处理
     *
     * @param venuePriceTemplateId 场馆价格模板ID
     * @param venueId 场馆ID（用于查询价格覆盖）
     * @param bookingDate 预订日期
     * @param slotTimes 槽位时间列表（开始时间）
     * @return 时间 -> 价格 的映射（不包含未找到价格的时间）
     */
    Map<LocalTime, BigDecimal> getSlotPriceMap(Long venuePriceTemplateId, Long venueId, LocalDate bookingDate, List<LocalTime> slotTimes);

    /**
     * 判断日期类型
     *
     * @param date 日期
     * @return 日期类型（工作日/周末/节假日）
     */
    DayType determineDayType(LocalDate date);

    /**
     * 计算额外费用
     * 查询场馆的额外费用配置并计算金额
     *
     * @param venueId 场馆ID
     * @param basePrice 基础价格（所有槽位价格总和）
     * @param slotCount 槽位数量
     * @return 额外费用列表
     */
    List<OrderLevelExtraQuote> calculateExtraCharges(Long venueId, BigDecimal basePrice, int slotCount);

    /**
     * 计算完整价格（槽位价格 + 额外费用）- 使用槽位模板
     * 用于预览场景，不需要实际占用槽位
     *
     * @param templates 槽位模板列表
     * @param venueId 场馆ID
     * @param venuePriceTemplateId 场馆价格模板ID
     * @param bookingDate 预订日期
     * @return 价格计算结果（只包含价格信息）
     */
    PricingInfo calculateCompletePricingByTemplates(
            List<VenueBookingSlotTemplate> templates,
            Long venueId,
            Long venuePriceTemplateId,
            LocalDate bookingDate);
}
