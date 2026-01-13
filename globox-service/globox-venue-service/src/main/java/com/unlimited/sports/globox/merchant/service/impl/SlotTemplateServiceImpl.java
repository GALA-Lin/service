package com.unlimited.sports.globox.merchant.service.impl;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.MerchantVenueBookingSlotTemplateMapper;
import com.unlimited.sports.globox.merchant.service.SlotTemplateService;
import com.unlimited.sports.globox.model.merchant.dto.BatchTemplateInitDto;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.merchant.vo.BatchTemplateInitResultVo;
import com.unlimited.sports.globox.venue.util.TimeSlotSplitUtil;
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
    private final CourtMapper courtMapper;

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

        // 使用工具类拆分时间槽位
        List<VenueBookingSlotTemplate> templates = new ArrayList<>();
        TimeSlotSplitUtil.splitTimeSlots(openTime, closeTime, SLOT_INTERVAL_MINUTES, slot -> {
            VenueBookingSlotTemplate template = VenueBookingSlotTemplate.builder()
                    .courtId(courtId)
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .build();
            templates.add(template);
        });

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
    /**
     * 批量初始化场地时段模板
     *
     * @param dto 批量初始化参数
     * @return 批量初始化结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchTemplateInitResultVo batchInitializeTemplates(BatchTemplateInitDto dto) {
        // 验证时间合法性
        if (dto.getOpenTime().isAfter(dto.getCloseTime()) ||
                dto.getOpenTime().equals(dto.getCloseTime())) {
            throw new GloboxApplicationException("开始时间必须早于结束时间");
        }

        int totalCourts = dto.getCourtIds().size();
        int successCourts = 0;
        int skippedCourts = 0;
        int failedCourts = 0;
        int totalTemplatesCreated = 0;

        List<BatchTemplateInitResultVo.CourtInitDetail> courtDetails = new ArrayList<>();

        for (Long courtId : dto.getCourtIds()) {
            BatchTemplateInitResultVo.CourtInitDetail detail =
                    BatchTemplateInitResultVo.CourtInitDetail.builder()
                            .courtId(courtId)
                            .build();

            try {
                // 查询场地信息
                Court court = courtMapper.selectById(courtId);
                if (court == null) {
                    detail.setCourtName("未知");
                    detail.setStatus("failed");
                    detail.setTemplatesCreated(0);
                    detail.setRemark("场地不存在");
                    failedCourts++;
                    courtDetails.add(detail);
                    continue;
                }

                detail.setCourtName(court.getName());

                // 检查是否已有模板
                List<VenueBookingSlotTemplate> existingTemplates =
                        templateMapper.selectByCourtIdOrderByTime(courtId);

                if (!existingTemplates.isEmpty()) {
                    if (dto.getOverwrite()) {
                        // 覆盖模式：先删除旧模板
                        templateMapper.deleteByCourtId(courtId);
                        log.info("场地ID: {} 删除了 {} 个旧模板", courtId, existingTemplates.size());
                    } else {
                        // 不覆盖：跳过
                        detail.setStatus("skipped");
                        detail.setTemplatesCreated(0);
                        detail.setRemark(String.format("已有%d个模板，跳过初始化", existingTemplates.size()));
                        skippedCourts++;
                        courtDetails.add(detail);
                        continue;
                    }
                }

                // 生成模板（使用工具类拆分时间槽位）
                List<VenueBookingSlotTemplate> templates = new ArrayList<>();
                TimeSlotSplitUtil.splitTimeSlots(dto.getOpenTime(), dto.getCloseTime(), SLOT_INTERVAL_MINUTES, slot -> {
                    VenueBookingSlotTemplate template = VenueBookingSlotTemplate.builder()
                            .courtId(courtId)
                            .startTime(slot.getStartTime())
                            .endTime(slot.getEndTime())
                            .build();
                    templates.add(template);
                });

                if (templates.isEmpty()) {
                    detail.setStatus("failed");
                    detail.setTemplatesCreated(0);
                    detail.setRemark("时间配置错误，未生成任何模板");
                    failedCourts++;
                } else {
                    // 批量插入
                    int count = templateMapper.batchInsert(templates);
                    detail.setStatus("success");
                    detail.setTemplatesCreated(count);
                    detail.setRemark(String.format("成功创建%d个时段模板", count));
                    successCourts++;
                    totalTemplatesCreated += count;

                    log.info("场地ID: {} ({}) 初始化成功，创建了 {} 个模板",
                            courtId, court.getName(), count);
                }

            } catch (Exception e) {
                log.error("场地ID: {} 初始化失败", courtId, e);
                detail.setStatus("failed");
                detail.setTemplatesCreated(0);
                detail.setRemark("初始化失败：" + e.getMessage());
                failedCourts++;
            }

            courtDetails.add(detail);
        }

        // 构建结果
        String message = String.format(
                "批量初始化完成：总共%d个场地，成功%d个，跳过%d个，失败%d个，共创建%d个模板",
                totalCourts, successCourts, skippedCourts, failedCourts, totalTemplatesCreated
        );

        return BatchTemplateInitResultVo.builder()
                .success(failedCourts == 0)
                .totalCourts(totalCourts)
                .successCourts(successCourts)
                .skippedCourts(skippedCourts)
                .failedCourts(failedCourts)
                .totalTemplatesCreated(totalTemplatesCreated)
                .message(message)
                .courtDetails(courtDetails)
                .build();
    }
}
