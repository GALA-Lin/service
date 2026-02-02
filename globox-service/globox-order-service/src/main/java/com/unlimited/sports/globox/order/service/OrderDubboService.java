package com.unlimited.sports.globox.order.service;

import com.unlimited.sports.globox.common.enums.order.SellerTypeEnum;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.order.dto.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单模块远程服务通用功能
 */
public interface OrderDubboService {


    /**
     * 服务提供方取消未支付的订单。
     *
     * @param orderNo    订单号
     * @param sellerId   服务提供方ID
     * @param operatorId 操作员ID
     * @param sellerType 服务提供方类型
     * @return 返回一个RpcResult对象，包含服务提供方取消订单的结果信息，包括订单号、是否取消成功、当前订单状态、状态描述以及取消时间
     */
    RpcResult<SellerCancelOrderResultDto> sellerCancelUnpaidOrder(Long orderNo, Long sellerId, Long operatorId, SellerTypeEnum sellerType);


    /**
     * 服务提供方确认订单的方法。
     *
     * @param orderNo     订单号
     * @param autoConfirm 是否自动确认
     * @param operatorId  操作员ID
     * @param sellerType  服务提供方类型
     * @return 返回服务提供方确认订单的结果，包括订单号、是否确认成功、当前订单状态、状态描述以及确认时间
     */
    RpcResult<SellerConfirmResultDto> sellerConfirm(Long orderNo, boolean autoConfirm, Long operatorId, SellerTypeEnum sellerType, Long sellerId);


    /**
     * 服务提供方批准退款申请。
     *
     * @param orderNo          订单号
     * @param sellerId         服务提供方ID
     * @param refundApplyId    退款申请ID
     * @param sellerType       服务提供方类型
     * @param refundPercentage 退款百分比
     * @return 返回一个RpcResult对象，包含服务提供方批准退款的结果信息，包括订单状态、退款申请状态等
     */
    RpcResult<SellerApproveRefundResultDto> sellerApproveRefund(Long orderNo, Long sellerId, Long refundApplyId, SellerTypeEnum sellerType, BigDecimal refundPercentage);


    /**
     * 服务提供方拒绝退款申请的方法。
     *
     * @param orderNo       订单号
     * @param refundApplyId 退款申请ID
     * @param sellerId      服务提供方ID
     * @param operatorId    操作员ID
     * @param sellerType    服务提供方类型
     * @param remark        备注信息，说明拒绝原因
     * @return 返回一个RpcResult对象，包含服务提供方拒绝退款的结果信息，包括订单状态、退款申请状态等
     */
    RpcResult<SellerRejectRefundResultDto> rejectRefund(Long orderNo, Long refundApplyId, Long sellerId, Long operatorId, SellerTypeEnum sellerType, String remark);


    /**
     * 服务提供方发起退款的方法。
     *
     * @param orderNo 订单号
     * @param SellerId 服务提供方ID
     * @param operatorId 操作员ID
     * @param reqItemIds 请求的订单项ID列表
     * @param sellerType 服务提供方类型
     * @param remark 备注信息，说明退款原因
     * @return 返回一个RpcResult对象，包含服务提供方发起退款的结果信息，包括订单状态、退款申请状态等
     */
    RpcResult<SellerRefundResultDto> refund(Long orderNo, Long SellerId, Long operatorId, List<Long> reqItemIds, SellerTypeEnum sellerType, String remark);
}
