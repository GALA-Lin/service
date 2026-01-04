package com.unlimited.sports.globox.dubbo.order;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.order.dto.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 订单服务对商家服务提供的退款方面 rpc 接口
 */
public interface OrderForMerchantRefundDubboService {

    /**
     * 商家同意退款的处理方法。
     *
     * @param dto 包含订单号、退款申请ID、场馆ID和商家ID等信息的请求参数
     * @return 返回商家同意退款的结果，包括订单状态、退款申请状态等信息
     */
    RpcResult<MerchantApproveRefundResultDto> merchantApproveRefund(
            @Valid @NotNull(message = "请求参数不能为空")
            MerchantApproveRefundRequestDto dto);


    /**
     * 商家拒绝退款的处理方法。
     *
     * @param dto 包含订单号、退款申请ID、场馆ID和商家ID等信息的请求参数
     * @return 返回商家拒绝退款的结果，包括订单状态、退款申请状态等信息
     */
    RpcResult<MerchantRejectRefundResultDto> merchantRejectRefund(
            @Valid @NotNull(message = "请求参数不能为空")
            MerchantRejectRefundRequestDto dto);

    /**
     * 商家退款申请详情（含items、extraCharges，可选timeline）
     *
     * @param dto 包含退款申请ID、订单号、商家ID以及场馆ID等信息的请求参数
     * @return 返回退款申请的详细信息，包括订单基本信息、退款申请状态、退款金额及与订单相关的项目和额外费用的退款明细
     */
    RpcResult<MerchantRefundApplyDetailsResultDto> merchantGetRefundApplyDetails(
            @Valid @NotNull(message = "请求参数不能为空")
            MerchantRefundApplyDetailsRequestDto dto);


    /**
     * 分页查询商家退款申请信息。
     *
     * @param dto 商家分页查询退款申请请求参数，包含商家ID、场馆ID列表、可选的退款申请状态、订单号、用户ID、申请时间范围、页码和每页大小
     * @return 返回分页后的退款申请信息列表，每个退款申请信息包括退款申请ID、订单号、用户ID、场馆ID、订单状态、退款申请状态、退款原因代码及详情、申请时间和审核时间、本次申请包含的退款项数量以及应退总额
     */
    RpcResult<IPage<MerchantRefundApplyPageResultDto>> merchantGetRefundApplyPage(
            @Valid @NotNull(message = "请求参数不能为空")
            MerchantRefundApplyPageRequestDto dto);


}
