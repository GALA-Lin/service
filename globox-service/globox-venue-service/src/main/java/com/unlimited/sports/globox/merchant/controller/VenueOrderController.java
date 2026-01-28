package com.unlimited.sports.globox.merchant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.dubbo.order.OrderForMerchantDubboService;
import com.unlimited.sports.globox.dubbo.order.OrderForMerchantRefundDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.*;
import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.merchant.util.MerchantAuthUtil;
import com.unlimited.sports.globox.model.merchant.dto.MerchantCancelOrderDto;
import com.unlimited.sports.globox.model.merchant.dto.MerchantGetOrderDetailsDto;
import com.unlimited.sports.globox.model.merchant.dto.MerchantGetOrderDto;
import com.unlimited.sports.globox.model.merchant.dto.MerchantRefundDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_MERCHANT_ACCOUNT_ID;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_MERCHANT_ID;
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
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @Valid MerchantGetOrderDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);
        MerchantGetOrderPageRequestDto requestDto = new MerchantGetOrderPageRequestDto();
        BeanUtils.copyProperties(dto, requestDto);
        // 如果指定了场馆ID，需要验证访问权限
        if (requestDto.getVenueId() != null) {
            merchantAuthUtil.validateVenueAccess(context, requestDto.getVenueId());
        }

        // 如果是员工且未指定场馆，则只查询自己场馆的订单
        if (context.isStaff() && requestDto.getVenueId() == null) {
            requestDto.setVenueId(context.getVenueId());
        }
        requestDto.setMerchantId(context.getMerchantId());
        RpcResult<IPage<MerchantGetOrderResultDto>> resultDto = orderForMerchantDubboService.getOrderPage(requestDto);
        Assert.rpcResultOk(resultDto);
        return R.ok(resultDto.getData());
    }

    /**
     * 查询订单详情
     */
    @Operation(summary = "查询订单详情")
    @GetMapping("/details")
    public R<MerchantGetOrderResultDto>  getOrderDetail(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @Valid MerchantGetOrderDetailsDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId,merchantId, roleStr);
        merchantAuthUtil.validateVenueAccess(context, dto.getVenueId());
        MerchantGetOrderDetailsRequestDto requestDto = MerchantGetOrderDetailsRequestDto.builder()
                .orderNo(dto.getOrderNo())
                .merchantId(merchantId)
                .venueId(dto.getVenueId())
                .build();
        // 如果是员工且未指定场馆，则只查询自己场馆的订单
        if (context.isStaff()) {
            requestDto.setVenueId(context.getVenueId());
        }
        requestDto.setMerchantId(context.getMerchantId());
        RpcResult<MerchantGetOrderResultDto> resultDto = orderForMerchantDubboService.getOrderDetails(requestDto);
        Assert.rpcResultOk(resultDto);
        return R.ok(resultDto.getData());
    }

    /**
     * 取消订单（全部/部分）
     */
    @Operation(summary = "取消订单（全部/部分）")
    @PostMapping("/cancel")
    public R<SellerCancelOrderResultDto> cancelOrder(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @RequestBody @Valid MerchantCancelOrderDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId,merchantId, roleStr);

        merchantAuthUtil.validateVenueAccess(context, dto.getVenueId());
        MerchantCancelOrderRequestDto requestDto = MerchantCancelOrderRequestDto.builder()
                .orderNo(dto.getOrderNo())
                .venueId(dto.getVenueId())
                .merchantId(merchantId)
                .build();
        // 如果是员工且未指定场馆，则只查询自己场馆的订单
        if (context.isStaff()) {
            requestDto.setVenueId(context.getVenueId());
        }
        requestDto.setMerchantId(context.getMerchantId());
        RpcResult<SellerCancelOrderResultDto> resultDto =  orderForMerchantDubboService.cancelUnpaidOrder(requestDto);
        Assert.rpcResultOk(resultDto);
        return R.ok(resultDto.getData());
    }

    /**
     * 商家发起退款
     */
    @Operation(summary = "商家发起退款")
    @PostMapping("/refund")
    public R<SellerRefundResultDto> refundOrder(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @RequestBody @Valid MerchantRefundDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId,merchantId, roleStr);
        merchantAuthUtil.validateVenueAccess(context, dto.getVenueId());

        MerchantRefundRequestDto requestDto = MerchantRefundRequestDto.builder()
                .merchantId(merchantId)
                .venueId(dto.getVenueId())
                .orderNo(dto.getOrderNo())
                .remark(dto.getRemark())
                .build();

        // 如果是员工且未指定场馆，则只查询自己场馆的订单
        if (context.isStaff()) {
            requestDto.setVenueId(context.getVenueId());
        }
        requestDto.setMerchantId(context.getMerchantId());
        RpcResult<SellerRefundResultDto> resultDto = orderForMerchantRefundDubboService.refund(requestDto);
        Assert.rpcResultOk(resultDto);
        return R.ok(resultDto.getData());
    }
}