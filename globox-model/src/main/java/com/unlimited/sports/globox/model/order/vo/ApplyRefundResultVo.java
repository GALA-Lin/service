package com.unlimited.sports.globox.model.order.vo;

import com.unlimited.sports.globox.common.enums.order.ApplyRefundStatusEnum;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 退款申请结果的值对象。
 * 包含了订单退款申请的相关信息，如订单号、退款申请ID、是否为整单退款等。
 */
@Data
@Builder
public class ApplyRefundResultVo implements Serializable {

    /**
     * 订单号
     */
    @NotNull
    private Long orderNo;

    /**
     * 退款申请ID
     */
    @NotNull
    private Long refundApplyId;

    /**
     * 当前订单状态
     */
    @NotNull
    private ApplyRefundStatusEnum applyStatus;

    /**
     * 申请时间
     */
    @NotNull
    private LocalDateTime appliedAt;
}
