package com.unlimited.sports.globox.model.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.unlimited.sports.globox.common.enums.order.ApplyRefundStatusEnum;
import com.unlimited.sports.globox.common.enums.order.RefundStatusEnum;
import com.unlimited.sports.globox.common.enums.order.UserRefundReasonEnum;
import com.unlimited.sports.globox.model.base.BaseEntity;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.*;

/**
 * 订单退款申请表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value ="order_refund_apply")
@EqualsAndHashCode(callSuper = true)
public class OrderRefundApply extends BaseEntity implements Serializable {

    private Long orderNo;

    /**
     * 申请状态
     */
    private ApplyRefundStatusEnum applyStatus;

    /**
     * 用户选择的退款原因
     */
    private UserRefundReasonEnum reasonCode;

    /**
     * 用户退款详细原因
     */
    private String reasonDetail;

    /**
     * 商家 / 教练审核时间
     */
    private LocalDateTime reviewedAt;

    /**
     * 商家 / 教练备注
     */
    private String sellerRemark;

    /**
     * 向支付平台申请退款的时间
     */
    private LocalDateTime refundInitiatedAt;

    /**
     * 支付平台退款到账时间
     */
    private LocalDateTime refundCompletedAt;

    /**
     * 支付平台返回的退款交易 ID
     */
    private String refundTransactionId;


    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}