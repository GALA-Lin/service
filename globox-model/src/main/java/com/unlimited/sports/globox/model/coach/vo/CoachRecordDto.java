package com.unlimited.sports.globox.model.coach.vo;

import com.unlimited.sports.globox.common.enums.order.RefundStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * @since 2026/1/12 13:50
 *
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoachRecordDto {
    /**
     * 记录 ID
     */
    @NotNull
    private Long recordId;

    /**
     * 订单ID
     */
    @NotNull
    private Long orderNo;

    /**
     * 教练 或 场地 ID
     */
    @NotNull
    private Long resourceId;

    /**
     * 教练 或 场地名称
     */
    @NotNull
    private String resourceName;

    /**
     * 预订日期
     */
    @NotNull
    private LocalDate bookingDate;

    /**
     * 开始时间
     */
    @NotNull
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @NotNull
    private LocalTime endTime;

    /**
     * 时段价格
     */
    @NotNull
    private BigDecimal unitPrice;

    /**
     * 退款状态
     */
    @Null
    private RefundStatusEnum status;

    /**
     * 状态名称
     */
    @Null
    private String statusName;

    /**
     * 是否可取消
     */
    @NotNull
    private Boolean cancelable;
}
