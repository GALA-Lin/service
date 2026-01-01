package com.unlimited.sports.globox.dubbo.order.dto;

import com.unlimited.sports.globox.common.enums.order.RefundStatusEnum;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 记录信息
 */
@Data
@Builder
public class RecordDto implements Serializable {

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
     * 场地ID
     */
    @NotNull
    private Long courtId;

    /**
     * 场地名称
     */
    @NotNull
    private String courtName;

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
