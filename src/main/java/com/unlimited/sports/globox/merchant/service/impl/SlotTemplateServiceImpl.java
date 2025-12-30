package com.unlimited.sports.globox.merchant.service.impl;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.merchant.mapper.MerchantVenueBookingSlotTemplateMapper;
import com.unlimited.sports.globox.merchant.service.SlotTemplateService;
import com.unlimited.sports.globox.model.merchant.entity.VenueBookingSlotTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 2025/12/27 15:02
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlotTemplateServiceImpl implements SlotTemplateService {

    private final MerchantVenueBookingSlotTemplateMapper templateMapper;

    private static final int SLOT_INTERVAL_MINUTES = 30;

    /**
     * 为场地初始化槽位模板（按30分钟切分）
     *
     * @param courtId   场地ID
     * @param openTime  开始时间
     * @param closeTime 结束时间
     * @return 创建的模板数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int initializeTemplatesForCourt(Long courtId, LocalTime openTime, LocalTime closeTime) {
        // 验证时间合法性
        if (openTime.isAfter(closeTime) || openTime.equals(closeTime)) {
            throw new GloboxApplicationException("开始时间必须早于结束时间");
        }
        // 查询已有模板
        List<VenueBookingSlotTemplate> existingTemplates = templateMapper.selectByCourtIdOrderByTime(courtId);
        if (!existingTemplates.isEmpty()) {
            log.warn("场地ID: {} 已有 {} 个模板，跳过初始化", courtId, existingTemplates.size());
            return 0;
        }
        List<VenueBookingSlotTemplate> templates = new ArrayList<>();
        LocalTime currentTime = openTime;

        while (currentTime.isBefore(closeTime)) {
            LocalTime endTime = currentTime.plusMinutes(SLOT_INTERVAL_MINUTES);
            if (endTime.isAfter(closeTime)) {
                break; // 最后一段超出营业时间，不创建
            }

            VenueBookingSlotTemplate template = VenueBookingSlotTemplate.builder()
                    .courtId(courtId)
                    .startTime(currentTime)
                    .endTime(endTime)
                    .build();

            templates.add(template);
            currentTime = endTime;
        }

        // 批量插入
        if (templates.isEmpty()) {
            log.warn("场地ID: {} 没有生成任何模板，请检查营业时间配置", courtId);
            return 0;
        }

        int count = templateMapper.batchInsert(templates);
        log.info("场地ID: {} 初始化完成，创建了 {} 个时段模板", courtId, count);
        return count;
    }

    /**
     * 获取场地的所有模板
     *
     * @param courtId 场地ID
     * @return 模板列表（按时间排序）
     */
    @Override
    public List<VenueBookingSlotTemplate> getTemplatesByCourtId(Long courtId) {
        return templateMapper.selectByCourtIdOrderByTime(courtId);
    }

    /**
     * 软删除场地的所有模板
     *
     * @param courtId 场地ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplatesByCourtId(Long courtId) {
        int count = templateMapper.deleteByCourtId(courtId);
        log.info("删除场地ID: {} 的所有模板，共 {} 个", courtId, count);
    }
}
