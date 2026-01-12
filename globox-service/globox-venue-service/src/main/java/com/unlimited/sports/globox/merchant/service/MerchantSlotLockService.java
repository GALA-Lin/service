package com.unlimited.sports.globox.merchant.service;

import com.unlimited.sports.globox.model.merchant.vo.LockedSlotVo;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * @since 2025/12/28 10:56
 * 商家锁场管理服务接口
 */

public interface MerchantSlotLockService {

    /**
     * 商家锁定单个时段
     */
    @Transactional(rollbackFor = Exception.class)
    void lockSlotByMerchant(Long templateId, LocalDate bookingDate, String reason, Long merchantId);


    @Transactional(rollbackFor = Exception.class)
    void lockSlotsBatchByMerchant(List<Long> templateIds, LocalDate bookingDate,
                                  String reason, Long merchantId);

    @Transactional(rollbackFor = Exception.class)
    void unlockSlotByMerchant(Long templateId, LocalDate bookingDate, Long merchantId);

    @Transactional(rollbackFor = Exception.class)
    void unlockSlotsBatchByMerchant(List<Long> templateIds, LocalDate bookingDate,
                                    Long merchantId);

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
