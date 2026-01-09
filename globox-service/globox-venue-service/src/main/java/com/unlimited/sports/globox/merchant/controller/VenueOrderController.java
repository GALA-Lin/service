package com.unlimited.sports.globox.merchant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.unlimited.sports.globox.common.result.R;
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

    private final VenueOrderService venueOrderService;
    private final MerchantAuthUtil merchantAuthUtil;

    /**
     * 分页查询商家订单列表
     *
     * @param employeeId 员工ID
     * @param roleStr    角色
     * @param queryDTO   查询条件
     * @return 分页订单列表
     */
    @GetMapping("/list")
    public R<IPage<VenueOrderVo>> queryOrders(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @Valid OrderQueryDto queryDTO) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        // 如果指定了场馆ID，需要验证访问权限
        if (queryDTO.getVenueId() != null) {
            merchantAuthUtil.validateVenueAccess(context, queryDTO.getVenueId());
        }

        // 如果是员工且未指定场馆，则只查询自己场馆的订单
        if (context.isStaff() && queryDTO.getVenueId() == null) {
            queryDTO.setVenueId(context.getVenueId());
        }

        IPage<VenueOrderVo> orderPage = venueOrderService.queryMerchantOrders(context.getMerchantId(), queryDTO);
        return R.ok(orderPage);
    }

    /**
     * 查询订单详情
     *
     * @param employeeId 员工ID
     * @param roleStr    角色
     * @param orderId    订单ID
     * @return 订单详情
     */
    @Operation(summary = "查询订单详情")
    @GetMapping("/{orderId}")
    public R<VenueOrderVo> getOrderDetail(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @PathVariable @NotNull(message = "订单ID不能为空") Long orderId) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        VenueOrderVo orderDetail = venueOrderService.getOrderDetail(context.getMerchantId(), orderId);

        // 如果是员工，验证订单是否属于自己的场馆
        if (context.isStaff() && orderDetail.getVenueId() != null) {
            merchantAuthUtil.validateVenueAccess(context, orderDetail.getVenueId());
        }

        return R.ok(orderDetail);
    }

    /**
     * 取消订单（全部/部分）
     *
     * @param employeeId 员工ID
     * @param roleStr    角色
     * @param cancelDTO  取消订单DTO
     * @return 操作结果
     */
    @Operation(summary = "取消订单（全部/部分）")
    @PostMapping("/cancel")
    public R<OrderCancelResultVo> cancelOrder(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @Valid @RequestBody OrderCancelDto cancelDTO) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        // 先查询订单信息以验证权限
        VenueOrderVo orderDetail = venueOrderService.getOrderDetail(context.getMerchantId(), cancelDTO.getOrderId());
        if (context.isStaff() && orderDetail.getVenueId() != null) {
            merchantAuthUtil.validateVenueAccess(context, orderDetail.getVenueId());
        }

        OrderCancelResultVo result = venueOrderService.cancelOrder(context.getMerchantId(), cancelDTO);
        return R.ok(result);
    }

    /**
     * 确认订单
     *
     * @param employeeId 员工ID
     * @param roleStr    角色
     * @param orderId    订单ID
     * @return 操作结果
     */
    @Operation(summary = "确认订单")
    @PostMapping("/{orderId}/confirm")
    public R<VenueOrderVo> confirmOrder(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @PathVariable @NotNull(message = "订单ID不能为空") Long orderId) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        // 先查询订单信息以验证权限
        VenueOrderVo orderDetail = venueOrderService.getOrderDetail(context.getMerchantId(), orderId);
        if (context.isStaff() && orderDetail.getVenueId() != null) {
            merchantAuthUtil.validateVenueAccess(context, orderDetail.getVenueId());
        }

        VenueOrderVo result = venueOrderService.confirmOrder(context.getMerchantId(), orderId);
        return R.ok(result);
    }

    /**
     * 获取订单统计数据
     *
     * @param employeeId 员工ID
     * @param roleStr    角色
     * @param venueId    场馆ID（可选）
     * @return 订单统计数据
     */
    @Operation(summary = "获取订单统计数据")
    @GetMapping("/statistics")
    public R<VenueOrderStatisticsVo> getStatistics(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestParam(required = false) Long venueId) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        // 如果指定了场馆ID，需要验证访问权限
        if (venueId != null) {
            merchantAuthUtil.validateVenueAccess(context, venueId);
        }

        // 如果是员工且未指定场馆，则只查询自己场馆的统计
        if (context.isStaff() && venueId == null) {
            venueId = context.getVenueId();
        }


        VenueOrderStatisticsVo statistics = venueOrderService.getOrderStatistics(context.getMerchantId(), venueId);
        return R.ok(statistics);
    }
}