package com.unlimited.sports.globox.venue.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 锁场结果DTO
 * 包含锁场ID映射和槽位价格列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockSlotsResult {

    /**
     * 每个槽位对应的第三方预订ID
     * key: SlotLockRequest（槽位请求）
     * value: 第三方预订ID（eventId或batchNo）
     */
    private Map<SlotLockRequest, String> bookingIds;

    /**
     * Away球场槽位价格列表
     * 包含所有可用槽位的详细价格信息
     */
    private List<AwaySlotPrice> slotPrices;

}
