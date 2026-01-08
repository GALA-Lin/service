package com.unlimited.sports.globox.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单错误码
 */
@Getter
@AllArgsConstructor
public enum OrderCode implements ResultCode {
    ORDER_CREATE_FAILED(1701, "订单创建失败"),
    ORDER_EXTRA_CHARGE_ERROR(1702, "额外费用计算有误"),
    ORDER_NOT_EXIST(1703, "订单不存在"),
    ORDER_ITEM_NOT_EXIST(1704, "当前订单不存在订单项"),
    SLOT_HAD_BOOKING(1705, "当前场地已被预订"),
    ORDER_STATUS_NOT_ALLOW_CANCEL(1706, "当前订单不允许被取消"),
    ORDER_CANCEL_FAILED(1707, "订单取消失败"),
    ORDER_NOT_ALLOW_REFUND(1708, "当前订单不允许退款"),
    ORDER_REFUND_ALREADY_PENDING(1709, "申请中存在正在退款的订单项，请勿重复提交"),
    ORDER_REFUND_ALREADY_REJECTED(1710, "申请中存在被拒绝退款的订单项，请联系提供方"),
    ORDER_REFUND_ALREADY_APPROVED(1711, "申请中存在已同意退款的订单项，请勿重复提交"),
    ORDER_REFUND_ALREADY_COMPLETED(1712, "申请中存在已完成退款的订单项，请勿重复提交"),
    ORDER_ITEM_REFUND_STATUS_INVALID(1713, "无效的退款状态码"),
    ORDER_REFUND_APPLY_NOT_EXIST(1714, "订单退款申请不存在"),
    ORDER_REFUND_APPLY_STATUS_NOT_ALLOW(1715, "该申请已被审批或取消"),
    ORDER_REFUND_APPLY_CREATE_FAILED(1716, "订单退款申请失败"),
    VENUE_ID_EMPTY(1718, "场馆列表为空"),
    COACH_ONLY_REFUND_ALL(1719, "教练订单不支持部分退款"),
    ORDER_CURRENT_NOT_ALLOW_PAY(1720, "当前订单不可支付"),


    ORDER_REFUND_ITEM_COUNT_ERROR(1721, "订单退款项有误"),
    ORDER_REFUND_AMOUNT_INVALID(1722, "订单退款金额无效"),
    PARAM_ERROR(1723, "参数错误");
    private final Integer code;
    private final String message;
}
