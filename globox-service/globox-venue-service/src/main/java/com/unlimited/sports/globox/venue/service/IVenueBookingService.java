package com.unlimited.sports.globox.venue.service;

import com.unlimited.sports.globox.dubbo.merchant.dto.PricingRequestDto;
import com.unlimited.sports.globox.dubbo.merchant.dto.PricingResultDto;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;

import java.util.List;
import java.util.Map;

public interface IVenueBookingService {


    /**
     * 在分布式锁保护下执行事务性的预订和计价逻辑
     * 此方法会开启事务，事务快照在获取锁之后建立，确保能读取到其他已提交事务的最新数据
     *
     * @param dto 价格请求DTO
     * @param templates 槽位模板列表
     * @param venue 场馆对象（外部已查询）
     * @param courtNameMap 场地名称映射
     * @param validateSlotsCallback 二次验证回调（复用外部的validateSlots方法）
     * @return 价格结果DTO
     */
     PricingResultDto executeBookingInTransaction(
            PricingRequestDto dto,
            List<VenueBookingSlotTemplate> templates,
            Venue venue,
            Map<Long, String> courtNameMap,
            Runnable validateSlotsCallback);
}
