package com.unlimited.sports.globox.dubbo.order;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.order.dto.*;
import com.unlimited.sports.globox.model.merchant.dto.RefundRequestDto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 订单服务对教练服务 提供订单相关接口
 */

public interface OrderForCoachDubboService {

    /**
     * 获取教练的订单分页信息。
     *
     * @param dto 请求参数，包含教练ID、页码和每页大小
     * @return 返回一个RpcResult对象，其中包含了分页后的订单信息列表。每个订单信息包括订单号、用户ID、场馆信息、价格明细、支付状态、订单状态等
     */
    RpcResult<IPage<CoachGetOrderResultDto>> getOrderPage(
            @Valid @NotNull(message = "请求参数不能为空")
            CoachGetOrderPageRequestDto dto);


    /**
     * 获取指定订单的详细信息。
     *
     * @param dto 请求参数，包含订单号和教练ID
     * @return 返回一个RpcResult对象，其中包含了订单的详细信息。订单详情包括但不限于订单号、用户ID、场馆信息、价格明细（基础价格、额外费用总和、小计、折扣金额、最终总价）、支付状态、订单状态、创建时间及订单时段列表等
     */
    RpcResult<CoachGetOrderResultDto> getOrderDetails(
            @Valid @NotNull(message = "请求参数不能为空")
            CoachGetOrderDetailsRequestDto dto);


    /**
     * 教练取消未支付订单。
     *
     * @param dto 包含订单号、商家ID以及可选的取消原因的请求参数
     * @return 取消订单的结果，包括订单号、当前订单状态、状态描述及取消时间
     */
    RpcResult<SellerCancelOrderResultDto> cancelUnpaidOrder(
            @Valid @NotNull(message = "请求参数不能为空")
            CoachCancelOrderRequestDto dto);

    /**
     * 教练确认订单的方法。
     *
     * @param dto 包含订单号的请求参数
     * @return 返回商家确认订单的结果，包括订单号、是否确认成功、当前订单状态、状态描述以及确认时间
     */
    RpcResult<SellerConfirmResultDto> confirm(CoachConfirmRequestDto dto);


    /**
     * 教练同意退款的处理方法。
     *
     * @param dto 包含订单号、退款申请ID、场馆ID和商家ID等信息的请求参数
     * @return 返回商家同意退款的结果，包括订单状态、退款申请状态等信息
     */
    RpcResult<SellerApproveRefundResultDto> approveRefund(
            @Valid @NotNull(message = "请求参数不能为空")
            CoachApproveRefundRequestDto dto);

    /**
     * 教练拒绝退款的处理方法。
     *
     * @param dto 包含订单号、退款申请ID、场馆ID和商家ID等信息的请求参数
     * @return 返回商家拒绝退款的结果，包括订单状态、退款申请状态等信息
     */
    RpcResult<SellerRejectRefundResultDto> rejectRefund(
            @Valid @NotNull(message = "请求参数不能为空")
            CoachRejectRefundRequestDto dto);


    /**
     * 教练退款处理方法。
     *
     * @param dto 包含订单号、退款申请ID、场馆ID和商家ID等信息的请求参数
     * @return 返回商家退款的结果，包括订单状态、退款申请状态等信息
     */
    RpcResult<SellerRefundResultDto> refund(
            @Valid @NotNull(message = "请求参数不能为空")
            CoachRefundRequestDto dto);
}
