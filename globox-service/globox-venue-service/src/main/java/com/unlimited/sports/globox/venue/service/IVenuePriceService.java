package com.unlimited.sports.globox.venue.service;

import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.venue.dto.DetailedPricingInfo;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.venue.adapter.dto.AwaySlotPrice;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 场馆价格服务
 * 专门处理价格查询和计算
 */
public interface IVenuePriceService {



    /**
     * 将Away槽位价格列表转换为DetailedPricingInfo格式
     * 用于统一Away和Home的价格返回格式
     *
     * @param awaySlotPrices Away槽位价格列表（来自第三方平台）
     * @param courtMap 场地映射表
     * @return 详细价格信息
     */
     DetailedPricingInfo convertAwaySlotPricesToDetailedPricingInfo(
             List<AwaySlotPrice> awaySlotPrices,
             List<VenueBookingSlotTemplate> templates,
             Map<Long, Court> courtMap);



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
     DetailedPricingInfo calculatePricingByCourtTemplates(
            List<VenueBookingSlotTemplate> templates,
            Long venueId,
            LocalDate bookingDate,
            Map<Long, Court> courtMap);
}
