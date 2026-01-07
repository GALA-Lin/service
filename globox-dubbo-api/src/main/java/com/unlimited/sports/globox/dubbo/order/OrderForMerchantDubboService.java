package com.unlimited.sports.globox.dubbo.order;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.order.dto.*;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 订单服务对商家服务提供订单相关 rpc 接口
 */
@Validated
public interface OrderForMerchantDubboService {

    /**
     * 商家分页查询用户订单信息。
     *
     * @param dto 商家分页查询订单请求参数，包含商家ID、页码和每页大小
     * @return 返回分页后的订单信息列表，每个订单信息包括订单号、用户ID、场馆信息、价格明细、支付状态、订单状态等
     */
    RpcResult<IPage<MerchantGetOrderResultDto>> getOrderPage(
            @Valid @NotNull(message = "请求参数不能为空")
            MerchantGetOrderPageRequestDto dto);


    /**
     * 获取订单详情
     *
     * @param dto 商家查询订单详情请求参数
     * @return 订单详情 Dto
     */
    RpcResult<MerchantGetOrderResultDto> getOrderDetails(
            @Valid @NotNull(message = "请求参数不能为空") MerchantGetOrderDetailsRequestDto dto);


    /**
     * 商家取消未支付订单。
     *
     * @param dto 包含订单号、商家ID以及可选的取消原因的请求参数
     * @return 取消订单的结果，包括订单号、当前订单状态、状态描述及取消时间
     */
    RpcResult<MerchantCancelOrderResultDto> cancelUnpaidOrder(
            @Valid @NotNull(message = "请求参数不能为空")
            MerchantCancelOrderRequestDto dto);

    /**
     * 商家确认订单的方法。
     *
     * @param dto 包含订单号的请求参数
     * @return 返回商家确认订单的结果，包括订单号、是否确认成功、当前订单状态、状态描述以及确认时间
     */
    RpcResult<MerchantConfirmResultDto> confirm(MerchantConfirmRequestDto dto);
}
