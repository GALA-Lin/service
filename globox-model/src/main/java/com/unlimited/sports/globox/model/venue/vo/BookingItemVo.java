package com.unlimited.sports.globox.model.venue.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 预订项明细
 * 公共VO，用于订单预览和订单详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingItemVo {

    /**
     * 场地信息快照
     */
    @NotNull
    private CourtSnapshotVo courtSnapshot;

    /**
     * 订单项基础金额
     */
    @NotNull
    private BigDecimal itemBaseAmount;

    /**
     * 订单项实际金额（含附加费用）
     */
    @NotNull
    private BigDecimal itemAmount;

    /**
     * 订单项附加费用
     */
    @NotNull
    private List<ExtraChargeVo> extraCharges;

    /**
     * 预订时间段列表
     */
    @NotNull
    private List<SlotBookingTime> slotBookingTimes;
}
