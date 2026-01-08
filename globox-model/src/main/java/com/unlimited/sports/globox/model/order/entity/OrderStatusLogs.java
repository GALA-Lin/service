package com.unlimited.sports.globox.model.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import com.unlimited.sports.globox.common.enums.order.OperatorTypeEnum;
import com.unlimited.sports.globox.common.enums.order.OrderActionEnum;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import com.unlimited.sports.globox.common.enums.order.RefundStatusEnum;
import com.unlimited.sports.globox.model.base.BaseEntity;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单状态流转日志表（支持订单级/订单项级/部分退款）
 */
@Data
@Builder
@TableName(value = "order_status_logs")
@EqualsAndHashCode(callSuper = true)
public class OrderStatusLogs extends BaseEntity {

    /**
     * 订单号
     */
    private Long orderNo;

    /**
     * 订单ID（可选，便于 join）
     */
    private Long orderId;

    /**
     * 订单明细ID（订单级变更可为空）
     */
    private Long orderItemId;

    /**
     * 业务动作
     */
    private OrderActionEnum action;

    /**
     * 原订单状态（orders.order_status）
     */
    private OrderStatusEnum oldOrderStatus;

    /**
     * 新订单状态（orders.order_status）
     */
    private OrderStatusEnum newOrderStatus;

    /**
     * 原退款状态（orders.refund_status，可选）
     */
    private RefundStatusEnum oldRefundStatus;

    /**
     * 新退款状态（orders.refund_status，可选）
     */
    private RefundStatusEnum newRefundStatus;

    /**
     * 原订单项退款状态（order_items.refund_status，可选）
     */
    private RefundStatusEnum oldItemRefundStatus;

    /**
     * 新订单项退款状态（order_items.refund_status，可选）
     */
    private RefundStatusEnum newItemRefundStatus;

    /**
     * 关联退款申请ID（order_refund_request.id）
     */
    private Long refundApplyId;

    /**
     * 关联订单项退款ID（order_item_refunds.id）
     */
    private Long itemRefundId;

    /**
     * 操作人类型：1=USER 2=MERCHANT 3=SYSTEM
     */
    private OperatorTypeEnum operatorType;

    /**
     * 操作人ID（userId/merchantId/系统任务ID等）
     */
    private Long operatorId;

    /**
     * 操作人名称（冗余快照）
     */
    private String operatorName;

    /**
     * 操作IP（可选）
     */
    private String operatorIp;

    /**
     * 变更快照/扩展字段（如退款金额、手续费、取消原因、第三方流水号等）
     */
    private Object extra;

    /**
     * 备注/原因描述
     */
    private String remark;
}