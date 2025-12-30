package com.unlimited.sports.globox.merchant.service;

import com.unlimited.sports.globox.model.merchant.entity.VenueBookingSlotTemplate;

import java.time.LocalTime;
import java.util.List;

/**
 * @since 2025/12/27 15:01
 * 槽位模板管理服务接口
 */

public interface SlotTemplateService {

    /**
     * 为场地初始化槽位模板（按30分钟切分）
     * @param courtId 场地ID
     * @param openTime 开始时间
     * @param closeTime 结束时间
     * @return 创建的模板数量
     */
    int initializeTemplatesForCourt(Long courtId, LocalTime openTime, LocalTime closeTime);

    /**
     * 获取场地的所有模板
     * @param courtId 场地ID
     * @return 模板列表（按时间排序）
     */
    List<VenueBookingSlotTemplate> getTemplatesByCourtId(Long courtId);

    /**
     * 软删除场地的所有模板
     * @param courtId 场地ID
     */
    void deleteTemplatesByCourtId(Long courtId);

}
