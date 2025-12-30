package com.unlimited.sports.globox.merchant.service;

import com.unlimited.sports.globox.merchant.controller.CourtManagementController;
import com.unlimited.sports.globox.model.merchant.dto.SlotLockRequestDto;
import com.unlimited.sports.globox.model.merchant.vo.BatchLockResultVo;
import com.unlimited.sports.globox.model.merchant.vo.BatchUnlockResultVo;
import com.unlimited.sports.globox.model.merchant.vo.LockedSlotVo;

import java.time.LocalDate;
import java.util.List;

/**
 * @since 2025/12/28 10:56
 * 商家锁场管理服务接口
 */
public interface MerchantSlotLockService {

    /**
     * 商家锁场（如果记录不存在则创建）
     *
     * @param templateId 时段模板ID
     * @param bookingDate 预订日期
     * @param reason 锁场原因
     * @param merchantId 商家ID
     * @return 创建或更新的记录ID
     */
    Long lockSlotByMerchant(Long templateId, LocalDate bookingDate, String reason, Long merchantId);

    /**
     * 批量锁场
     *
     * @param requests 锁场请求列表
     * @param reason 锁场原因
     * @param merchantId 商家ID
     * @return 批量操作结果
     */
    BatchLockResultVo lockSlotsBatchByMerchant(List<SlotLockRequestDto> requests, String reason, Long merchantId);

    /**
     * 商家解锁
     *
     * @param recordId 记录ID
     * @param merchantId 商家ID
     */
    void unlockSlotByMerchant(Long recordId, Long merchantId);

    /**
     * 批量解锁
     *
     * @param recordIds 记录ID列表
     * @param merchantId 商家ID
     * @return 批量操作结果
     */
    BatchUnlockResultVo unlockSlotsBatchByMerchant(List<Long> recordIds, Long merchantId);

    /**
     * 查询锁定的时段
     *
     * @param courtId 场地ID（可选）
     * @param venueId 场馆ID（可选）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param lockedType 锁定类型（可选）
     * @return 锁定时段列表
     */
    List<LockedSlotVo> queryLockedSlots(Long courtId, Long venueId,
                                        LocalDate startDate, LocalDate endDate,
                                        Integer lockedType);
}