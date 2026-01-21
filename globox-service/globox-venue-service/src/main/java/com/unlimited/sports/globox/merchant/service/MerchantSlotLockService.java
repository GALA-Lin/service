package com.unlimited.sports.globox.merchant.service;

import com.unlimited.sports.globox.model.merchant.vo.LockedSlotVo;

import java.time.LocalDate;
import java.util.List;

/**
 * 商家锁场服务接口
 */
public interface MerchantSlotLockService {

    /**
     * 商家锁定单个时段
     *
     * @param templateId  槽位模板ID
     * @param bookingDate 预约日期
     * @param reason      锁定原因
     * @param userName    使用人姓名（可选）
     * @param userPhone   使用人手机号（可选）
     * @param merchantId  商家ID
     * @param id
     */
    void lockSlotByMerchant(Long templateId, LocalDate bookingDate, String reason,
                            String userName, String userPhone, Long merchantId, Long id);

    /**
     * 商家批量锁定时段（同一批次）
     *
     * @param templateIds 槽位模板ID列表
     * @param bookingDate 预约日期
     * @param reason      锁定原因
     * @param userName    使用人姓名（可选）
     * @param userPhone   使用人手机号（可选）
     * @param merchantId  商家ID
     * @param id
     */
    void lockSlotsBatchByMerchant(List<Long> templateIds, LocalDate bookingDate, String reason,
                                  String userName, String userPhone, Long merchantId, Long id);

    /**
     * 商家解锁单个时段
     *
     * @param templateId  槽位模板ID
     * @param bookingDate 预约日期
     * @param merchantId  商家ID
     */
    void unlockSlotByMerchant(Long templateId, LocalDate bookingDate, Long merchantId);

    /**
     * 商家批量解锁时段
     *
     * @param templateIds 槽位模板ID列表
     * @param bookingDate 预约日期
     * @param merchantId  商家ID
     */
    void unlockSlotsBatchByMerchant(List<Long> templateIds, LocalDate bookingDate, Long merchantId);

    /**
     * 查询锁定的时段
     *
     * @param courtId     场地ID（可选）
     * @param venueId     场馆ID（可选）
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param lockedByType 锁定类型（可选）
     * @return 锁定时段列表
     */
    List<LockedSlotVo> queryLockedSlots(Long courtId, Long venueId,
                                        LocalDate startDate, LocalDate endDate,
                                        Integer lockedByType);
}