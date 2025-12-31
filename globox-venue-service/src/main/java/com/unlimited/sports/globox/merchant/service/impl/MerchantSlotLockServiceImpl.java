package com.unlimited.sports.globox.merchant.service.impl;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.merchant.mapper.*;
import com.unlimited.sports.globox.merchant.service.MerchantSlotLockService;
import com.unlimited.sports.globox.model.merchant.entity.*;
import com.unlimited.sports.globox.model.merchant.enums.SlotRecordStatusEnum;
import com.unlimited.sports.globox.model.merchant.vo.LockedSlotVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static com.unlimited.sports.globox.model.merchant.enums.OperatorSourceEnum.*;
import static com.unlimited.sports.globox.model.merchant.enums.SlotRecordStatusEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantSlotLockServiceImpl implements MerchantSlotLockService {

    private final MerchantVenueBookingSlotRecordMapper recordMapper;
    private final MerchantVenueBookingSlotTemplateMapper templateMapper;
    private final CourtMapper courtMapper;
    private final VenueMapper venueMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockSlotByMerchant(Long recordId, String reason, Long merchantId) {
        // TODO 查询有无记录，无插入，有更改
        // 1. 查询记录
        VenueBookingSlotRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new GloboxApplicationException("槽位记录不存在");
        }

        // 2. 权限校验
        validateMerchantPermission(record.getSlotTemplateId(), merchantId);

        // 3. 状态校验
        if (!record.getStatus().equals(AVAILABLE.getCode())) {
            throw new GloboxApplicationException(
                    String.format("该时段当前状态为【%s】，无法锁定",
                            getStatusName(record.getStatus()))
            );
        }

        // 4. 更新状态
        record.setStatus(UNAVAILABLE.getCode());
        record.setLockedType(MERCHANT.getCode()); // 2=商家锁场
        record.setLockReason(reason);
        recordMapper.updateById(record);

        log.info("商家ID: {} 锁定了记录ID: {}, 原因: {}", merchantId, recordId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockSlotsBatchByMerchant(List<Long> recordIds, String reason, Long merchantId) {
        if (recordIds == null || recordIds.isEmpty()) {
            throw new GloboxApplicationException("请选择要锁定的时段");
        }

        // Todo: 状态校验 Redis ETA 2026-01-15
        // 批量锁定
        int successCount = 0;
        for (Long recordId : recordIds) {
            try {
                lockSlotByMerchant(recordId, reason, merchantId);
                successCount++;
            } catch (Exception e) {
                log.warn("锁定记录ID: {} 失败: {}", recordId, e.getMessage());
            }
        }

        log.info("商家ID: {} 批量锁定完成，成功: {}/{}",merchantId, successCount, recordIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockSlotByMerchant(Long recordId, Long merchantId) {
        // 1. 查询记录
        VenueBookingSlotRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new GloboxApplicationException("槽位记录不存在");
        }

        // 2. 权限校验
        validateMerchantPermission(record.getSlotTemplateId(), merchantId);

        // 3. 状态校验
        if (!record.getStatus().equals(UNAVAILABLE.getCode())) {
            throw new GloboxApplicationException("该时段未被商家锁定，无需解锁");
        }

        if (record.getLockedType() == null || !record.getLockedType().equals(2)) {
            throw new GloboxApplicationException("该时段不是商家锁定的，无法解锁");
        }

        // 4. 更新状态
        record.setStatus(AVAILABLE.getCode());
        record.setLockedType(null);
        record.setLockReason(null);
        recordMapper.updateById(record);

        log.info("商家ID: {} 解锁了记录ID: {}", merchantId, recordId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockSlotsBatchByMerchant(List<Long> recordIds, Long merchantId) {
        if (recordIds == null || recordIds.isEmpty()) {
            throw new GloboxApplicationException("请选择要解锁的时段");
        }

        int successCount = 0;
        for (Long recordId : recordIds) {
            try {
                unlockSlotByMerchant(recordId, merchantId);
                successCount++;
            } catch (Exception e) {
                log.warn("解锁记录ID: {} 失败: {}", recordId, e.getMessage());
            }
        }

        log.info("商家ID: {} 批量解锁完成，成功: {}/{}", merchantId, successCount, recordIds.size());
    }


    /**
     * 校验商家权限
     */
    private void validateMerchantPermission(Long templateId, Long merchantId) {
        VenueBookingSlotTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new GloboxApplicationException("槽位模板不存在");
        }

        Court court = courtMapper.selectById(template.getCourtId());
        if (court == null) {
            throw new GloboxApplicationException("场地不存在");
        }

        Venue venue = venueMapper.selectById(court.getVenueId());
        if (venue == null || !venue.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该时段");
        }
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer statusCode) {
        SlotRecordStatusEnum status = SlotRecordStatusEnum.getByCode(statusCode);
        return status != null ? status.getName() : "未知";
    }

    @Override
    public List<LockedSlotVo> queryLockedSlots(Long courtId, Long venueId,
                                               LocalDate startDate, LocalDate endDate,
                                               Integer lockedByType) {
        // 参数验证
        if (courtId == null && venueId == null) {
            throw new GloboxApplicationException("场地ID和场馆ID至少提供一个");
        }

        if (startDate == null || endDate == null) {
            throw new GloboxApplicationException("开始日期和结束日期不能为空");
        }

        if (startDate.isAfter(endDate)) {
            throw new GloboxApplicationException("开始日期不能晚于结束日期");
        }

        // 查询数据
        List<LockedSlotVo> result;
        if (courtId != null) {
            // 按场地查询
            result = recordMapper.selectLockedSlotsByCourtAndDateRange(courtId, startDate, endDate);
        } else {
            // 按场馆查询
            result = recordMapper.selectLockedSlotsByVenueAndDateRange(venueId, startDate, endDate);
        }

        // 过滤锁定类型
        if (lockedByType != null) {
            result = result.stream()
                    .filter(vo -> vo.getLockedBy().equals(lockedByType))
                    .collect(Collectors.toList());
        }

        log.info("查询锁定时段，场地ID: {}, 场馆ID: {}, 日期范围: {}~{}, 锁定类型: {}, 结果数: {}",
                courtId, venueId, startDate, endDate, lockedByType, result.size());

        return result;
    }
}