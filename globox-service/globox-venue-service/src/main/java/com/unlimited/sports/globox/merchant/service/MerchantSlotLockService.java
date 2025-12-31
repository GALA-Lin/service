package com.unlimited.sports.globox.merchant.service;

import com.unlimited.sports.globox.model.merchant.vo.LockedSlotVo;

import java.time.LocalDate;
import java.util.List;

/**
 * @since 2025/12/28 10:56
 * 商家锁场管理服务接口
 */

public interface MerchantSlotLockService {

    /**
     * 商家锁定单个时段
     * @param recordId 记录ID
     * @param reason 锁场原因（如："场地维护"、"私人使用"、"设备检修"）
     * @param merchantId 商家ID（用于权限校验）
     */
    void lockSlotByMerchant(Long recordId, String reason, Long merchantId);

    /**
     * 商家批量锁定时段
     * @param recordIds 记录ID列表
     * @param reason 锁场原因
     * @param merchantId 商家ID
     */
    void lockSlotsBatchByMerchant(List<Long> recordIds, String reason, Long merchantId);


    /**
     * 商家解锁时段
     * @param recordId 记录ID
     * @param merchantId 商家ID
     */
    void unlockSlotByMerchant(Long recordId, Long merchantId);

    /**
     * 商家批量解锁时段
     * @param recordIds 记录ID列表
     * @param merchantId 商家ID
     */
    void unlockSlotsBatchByMerchant(List<Long> recordIds, Long merchantId);

    /**
     * 查询商家已锁定的时段
     * @param courtId 场地ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 锁定记录列表
     */
    List<LockedSlotVo> queryLockedSlots(Long courtId, Long venueId,
                                        LocalDate startDate, LocalDate endDate,
                                        Integer lockedByType);
}
