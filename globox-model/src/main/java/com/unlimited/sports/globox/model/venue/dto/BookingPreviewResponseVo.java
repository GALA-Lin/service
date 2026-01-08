package com.unlimited.sports.globox.model.venue.dto;

import com.unlimited.sports.globox.model.venue.vo.BookingItemVo;
import com.unlimited.sports.globox.model.venue.vo.ExtraChargeVo;
import com.unlimited.sports.globox.model.venue.vo.VenueSnapshotVo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 预订预览响应VO
 * 包含场馆信息和价格计算结果
 * 用于用户在下单前预览订单信息（不锁槽位）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingPreviewResponseVo {

    /**
     * 场馆信息快照
     */
    @NotNull
    private VenueSnapshotVo venueSnapshot;

    /**
     * 订单总金额
     */
    @NotNull
    private BigDecimal amount;

    /**
     * 预订日期
     */
    @NotNull
    private LocalDate bookingDate;

    /**
     * 是否活动订单
     */
    @NotNull
    private Boolean isActivity;

    /**
     * 活动类型名称
     */
    private String activityTypeName;

    /**
     * 订单级附加费用列表
     */
    @NotNull
    private List<ExtraChargeVo> orderLevelExtraCharges;

    /**
     * 预订项列表（按场地分组）
     */
    @NotNull
    private List<BookingItemVo> items;
}
