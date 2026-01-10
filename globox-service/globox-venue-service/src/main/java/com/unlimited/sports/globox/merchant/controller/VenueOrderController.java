package com.unlimited.sports.globox.merchant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.dubbo.order.OrderForCoachDubboService;
import com.unlimited.sports.globox.dubbo.order.OrderForMerchantDubboService;
import com.unlimited.sports.globox.dubbo.order.OrderForMerchantRefundDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.*;
import com.unlimited.sports.globox.merchant.service.VenueOrderService;
import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.merchant.util.MerchantAuthUtil;
import com.unlimited.sports.globox.model.merchant.dto.OrderCancelDto;
import com.unlimited.sports.globox.model.merchant.dto.OrderQueryDto;
import com.unlimited.sports.globox.model.merchant.vo.OrderCancelResultVo;
import com.unlimited.sports.globox.model.merchant.vo.VenueOrderStatisticsVo;
import com.unlimited.sports.globox.model.merchant.vo.VenueOrderVo;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_EMPLOYEE_ID;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_MERCHANT_ROLE;


/**
 * @author Linsen Hu
 * @since 2025/12/22 12:13
 * 商家订单管理controller
 */
@Slf4j
@RestController
@RequestMapping("/merchant/orders")
@RequiredArgsConstructor
@Validated
public class VenueOrderController {

    @DubboReference(group = "rpc")
    private OrderForMerchantDubboService orderForMerchantDubboService;
    @DubboReference(group = "rpc")
    private OrderForMerchantRefundDubboService orderForMerchantRefundDubboService;
    private final MerchantAuthUtil merchantAuthUtil;

    /**
     * 分页查询商家订单列表
     */
    @GetMapping("/list")
    public R<IPage<MerchantGetOrderResultDto>> getOrderPage(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @Valid MerchantGetOrderPageRequestDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        // 如果指定了场馆ID，需要验证访问权限
        if (dto.getVenueId() != null) {
            merchantAuthUtil.validateVenueAccess(context, dto.getVenueId());
        }

        // 如果是员工且未指定场馆，则只查询自己场馆的订单
        if (context.isStaff() && dto.getVenueId() == null) {
            dto.setVenueId(context.getVenueId());
        }
        RpcResult<IPage<MerchantGetOrderResultDto>> resultDto = orderForMerchantDubboService.getOrderPage(dto);
        Assert.rpcResultOk(resultDto);
        return R.ok(resultDto.getData());
    }

    /**
     * 查询订单详情
     */
    @Operation(summary = "查询订单详情")
    @GetMapping("/details")
    public R<MerchantGetOrderResultDto>  getOrderDetail(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @Valid MerchantGetOrderDetailsRequestDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        RpcResult<MerchantGetOrderResultDto> resultDto = orderForMerchantDubboService.getOrderDetails(dto);
        Assert.rpcResultOk(resultDto);
        return R.ok(resultDto.getData());
    }


    /**
     * 取消订单（全部/部分）
     */
    @Operation(summary = "取消订单（全部/部分）")
    @PostMapping("/cancel")
    public R<SellerCancelOrderResultDto> cancelOrder(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestBody @Valid MerchantCancelOrderRequestDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);
        RpcResult<SellerCancelOrderResultDto> resultDto =  orderForMerchantDubboService.cancelUnpaidOrder(dto);
        Assert.rpcResultOk(resultDto);
        return R.ok(resultDto.getData());
    }

    /**
     * 商家发起退款
     */
    @Operation(summary = "商家发起退款")
    @PostMapping("/refund")
    public R<SellerRefundResultDto> refundOrder(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestBody @Valid MerchantRefundRequestDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        RpcResult<SellerRefundResultDto> resultDto = orderForMerchantRefundDubboService.refund(dto);
        Assert.rpcResultOk(resultDto);
        return R.ok(resultDto.getData());
    }
}