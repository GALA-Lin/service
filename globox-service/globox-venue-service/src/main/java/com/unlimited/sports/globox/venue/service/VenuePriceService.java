package com.unlimited.sports.globox.venue.service;

import com.unlimited.sports.globox.dubbo.merchant.dto.OrderLevelExtraQuote;
import com.unlimited.sports.globox.dubbo.merchant.dto.RecordQuote;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplate;
import com.unlimited.sports.globox.model.venue.enums.DayType;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 场馆价格服务
 * 专门处理价格查询和计算
 */
public interface VenuePriceService {

    /**
     * 查询单个时间点的价格
     *
     * @param venueId 场馆ID
     * @param bookingDate 预订日期
     * @param slotTime 槽位时间（开始时间）
     * @return 价格，如果未找到则返回 null
     */
    BigDecimal getSlotPrice(Long venueId, LocalDate bookingDate, LocalTime slotTime);

    /**
     * 批量查询多个时间点的价格
     * 用于场馆搜索和槽位查询
     *
     * @param venueId 场馆ID
     * @param bookingDate 预订日期
     * @param slotTimes 槽位时间列表（开始时间）
     * @return 时间 -> 价格 的映射（不包含未找到价格的时间）
     */
    Map<LocalTime, BigDecimal> getSlotPriceMap(Long venueId, LocalDate bookingDate, List<LocalTime> slotTimes);

    /**
     * 批量查询多个时间点的价格（字符串版本）
     * 用于兼容不同的使用场景
     *
     * @param venueId 场馆ID
     * @param bookingDate 预订日期
     * @param slotTimeStrings 槽位时间列表（字符串格式：HH:mm:ss）
     * @return 时间字符串 -> 价格 的映射
     */
    Map<String, BigDecimal> getSlotPriceMapByString(Long venueId, LocalDate bookingDate, List<String> slotTimeStrings);

    /**
     * 获取价格模板
     *
     * @param venueId 场馆ID
     * @return 价格模板，如果未配置则返回 null
     */
    VenuePriceTemplate getPriceTemplate(Long venueId);

    /**
     * 判断日期类型
     *
     * @param date 日期
     * @return 日期类型（工作日/周末/节假日）
     */
    DayType determineDayType(LocalDate date);

    /**
     * 计算槽位价格并构建SlotQuote列表
     * 统一处理槽位价格查询、计算和VO构建
     *
     * @param templates 槽位模板列表
     * @param venueId 场馆ID
     * @param venueName 场馆名称
     * @param bookingDate 预订日期
     * @param courtMap 场地ID -> 场地信息的映射
     * @return 槽位价格报价列表
     */
    List<RecordQuote> calculateSlotQuotes(List<VenueBookingSlotTemplate> templates,
                                        Long venueId,
                                        String venueName,
                                        LocalDate bookingDate,
                                        Map<Long, String> courtMap);

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
}
