package com.unlimited.sports.globox.model.merchant.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @since 2025/12/29 17:44
 * 批量锁场结果
 */

@Data
@Builder
public  class BatchLockResultVo {
    /**
     * 总数
     */
    private Integer totalCount;

    /**
     * 成功数
     */
    private Integer successCount;

    /**
     * 失败数
     */
    private Integer failed;

    /**
     * 已被预订的数量
     */
    private Integer bookedCount;

    /**
     * 错误信息列表
     */
    private List<String> errorMessages;

    /**
     * 已被预订的时段详情
     */
    private List<BookedSlotInfoVo> bookedSlots;
}