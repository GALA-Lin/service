package com.unlimited.sports.globox.dubbo.order;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.unlimited.sports.globox.dubbo.order.dto.*;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 订单 dubbo service 接口
 */
@Validated
public interface OrderDubboService {

    /**
     * 商家分页查询用户订单信息。
     *
     * @param dto 商家分页查询订单请求参数，包含商家ID、页码和每页大小
     * @return 返回分页后的订单信息列表，每个订单信息包括订单号、用户ID、场馆信息、价格明细、支付状态、订单状态等
     */
    IPage<MerchantGetOrderResultDto> merchantGetOrderPage(
            @Valid @NotNull(message = "请求参数不能为空")
            MerchantGetOrderPageRequestDto dto);


    /**
     * 获取订单详情
     *
     * @param dto 商家查询订单详情请求参数
     * @return 订单详情 Dto
     */
    MerchantGetOrderResultDto merchantGetOrderDetails(
            @Valid @NotNull(message = "请求参数不能为空") MerchantGetOrderDetailsRequestDto dto);


    /**
     * 商家取消未支付订单。
     *
     * @param dto 包含订单号、商家ID以及可选的取消原因的请求参数
     * @return 取消订单的结果，包括订单号、当前订单状态、状态描述及取消时间
     */
    MerchantCancelOrderResultDto merchantCancelUnpaidOrder(
            @Valid @NotNull(message = "请求参数不能为空")
            MerchantCancelOrderRequestDto dto);

    /**
     * 商家同意退款的处理方法。
     *
     * @param dto 包含订单号、退款申请ID、场馆ID和商家ID等信息的请求参数
     * @return 返回商家同意退款的结果，包括订单状态、退款申请状态等信息
     */
    MerchantApproveRefundResultDto merchantApproveRefund(
            @Valid @NotNull(message = "请求参数不能为空")
            MerchantApproveRefundRequestDto dto);


    /**
     * 商家拒绝退款的处理方法。
     *
     * @param dto 包含订单号、退款申请ID、场馆ID和商家ID等信息的请求参数
     * @return 返回商家拒绝退款的结果，包括订单状态、退款申请状态等信息
     */
    MerchantRejectRefundResultDto merchantRejectRefund(
            @Valid @NotNull(message = "请求参数不能为空")
            MerchantRejectRefundRequestDto dto);


    /**
     * 分页查询商家退款申请信息。
     *
     * @param dto 商家分页查询退款申请请求参数，包含商家ID、场馆ID列表、可选的退款申请状态、订单号、用户ID、申请时间范围、页码和每页大小
     * @return 返回分页后的退款申请信息列表，每个退款申请信息包括退款申请ID、订单号、用户ID、场馆ID、订单状态、退款申请状态、退款原因代码及详情、申请时间和审核时间、本次申请包含的退款项数量以及应退总额
     */
    IPage<MerchantRefundApplyPageResultDto> merchantGetRefundApplyPage(
            @Valid @NotNull(message = "请求参数不能为空")
            MerchantRefundApplyPageRequestDto dto);

    /**
     * 商家退款申请详情（含items、extraCharges，可选timeline）
     *
     * @param dto 包含退款申请ID、订单号、商家ID以及场馆ID等信息的请求参数
     * @return 返回退款申请的详细信息，包括订单基本信息、退款申请状态、退款金额及与订单相关的项目和额外费用的退款明细
     */
    MerchantRefundApplyDetailsResultDto merchantGetRefundApplyDetails(
            @Valid @NotNull(message = "请求参数不能为空")
            MerchantRefundApplyDetailsRequestDto dto);


    /**
     * 商家确认订单的方法。
     *
     * @param dto 包含订单号的请求参数
     * @return 返回商家确认订单的结果，包括订单号、是否确认成功、当前订单状态、状态描述以及确认时间
     */
    MerchantConfirmResultDto merchantConfirm(MerchantConfirmRequestDto dto);


    /**
     * payment 支付前查询订单情况
     *
     * @param orderNo 订单编号
     * @return 订单相关信息
     */
    PaymentGetOrderResultDto paymentGetOrders(Long orderNo);
}
