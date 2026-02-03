package com.unlimited.sports.globox.order.controller;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.order.dto.*;
import com.unlimited.sports.globox.model.order.vo.*;
import com.unlimited.sports.globox.order.service.OrderRefundService;
import com.unlimited.sports.globox.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 订单相关接口 - 控制层
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "订单接口", description = "订单创建、查询、取消、退款及退款进度相关接口")
public class OrderController {

    private final OrderService orderService;
    private final OrderRefundService orderRefundService;

    /**
     * 用户创建订场订单
     */
    @PostMapping("venues")
    @Operation(summary = "创建订场订单", description = "用户选择场地和时间段创建订场订单")
    public R<CreateOrderResultVo> createVenueOrder(
            @Valid
            @RequestBody
            @Parameter(description = "创建订场订单请求参数", required = true)
            CreateVenueOrderDto dto,
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {

        CreateOrderResultVo result = orderService.createVenueOrder(dto, userId);
        log.info("[用户创建订单] 用户创建订场订单成功: userId:{} orderNo:{}", userId, result.getOrderNo());
        return R.ok(result);
    }


    /**
     * 用户创建活动订单
     */
    @PostMapping("venues/activity")
    @Operation(summary = "创建活动订单", description = "用户基于场馆活动创建订单")
    public R<CreateOrderResultVo> createVenueActivityOrder(
            @Valid
            @RequestBody
            @Parameter(description = "创建活动订单请求参数", required = true)
            CreateVenueActivityOrderDto dto,
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {

        CreateOrderResultVo result = orderService.createVenueActivityOrder(dto, userId);
        log.info("[用户创建订单] 用户创建活动订场订单成功: userId:{} orderNo:{}", userId, result.getOrderNo());
        return R.ok(result);
    }


    /**
     * 用户创建教练订单
     */
    @PostMapping("venues/coach")
    @Operation(summary = "创建教练订单", description = "用户创建教练订单")
    public R<CreateOrderResultVo> createCoachOrder(
            @Valid
            @RequestBody
            @Parameter(description = "创建活动订单请求参数", required = true)
            CreateCoachOrderDto dto,
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {

        CreateOrderResultVo result = orderService.createCoachOrder(dto, userId);
        log.info("[用户创建订单] 用户创建教练订单成功: userId:{} orderNo:{}", userId, result.getOrderNo());
        return R.ok(result);
    }


    /**
     * 用户获取订单列表
     */
    @GetMapping
    @Operation(summary = "获取订单列表", description = "分页获取当前用户的订单列表")
    public R<PaginationResult<GetOrderVo>> getOrderPage(
            @Validated @Parameter(description = "订单分页查询参数") GetOrderPageDto pageDto,
            @RequestHeader(value = RequestHeaderConstants.HEADER_THIRD_PARTY_OPENID, required = false) String openId) {
        PaginationResult<GetOrderVo> resultList = orderService.getOrderPage(pageDto, openId);
        return R.ok(resultList);
    }


    /**
     * 用户获取订单详情
     */
    @GetMapping("{orderNo}")
    @Operation(summary = "获取订单详情", description = "根据订单号获取订单详情（可传入用户地理位置用于距离计算）")
    public R<GetOrderDetailsVo> getOrderDetails(
            @PathVariable @Parameter(description = "订单号", required = true, example = "202512180001") Long orderNo,
            @ModelAttribute @Valid @Parameter(description = "用户地理位置信息") GetOrderDetailsDto dto) {
        dto.setOrderNo(orderNo);
        GetOrderDetailsVo resultVo = orderService.getDetails(dto);
        return R.ok(resultVo);
    }

    /**
     * 取消指定编号的订单
     */
    @PostMapping("/{orderNo}/cancel")
    @Operation(summary = "取消订单", description = "用户取消未支付或可取消状态的订单")
    public R<CancelOrderResultVo> cancelOrder(
            @PathVariable("orderNo")
            @NotNull(message = "订单号不能为空")
            @Parameter(description = "订单号", required = true, example = "202512180001")
            Long orderNo,
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        CancelOrderResultVo resultVo = orderService.cancelUnpaidOrder(orderNo, userId);
        log.info("[用户手动取消订单] 用户取消订单成功 userId:{} orderNo:{}", userId, orderNo);
        return R.ok(resultVo);
    }

    /**
     * 用户申请订单退款
     */
    @PostMapping("/refund/apply")
    @Operation(summary = "申请订单退款", description = "用户对订单或订单项发起退款申请")
    public R<ApplyRefundResultVo> applyRefund(
            @Valid
            @RequestBody
            @Parameter(description = "退款申请参数", required = true)
            ApplyRefundRequestDto dto,
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        ApplyRefundResultVo resultVo = orderRefundService.applyRefund(dto);
        log.info("[用户申请退款] 用户申请订单退款成功 userId:{} orderNo:{} refundApplyId:{} isRefundable:{} isAutoRefund:{} applyStatus:{}",
                userId,
                dto.getOrderNo(),
                resultVo.getRefundApplyId(),
                resultVo.isRefundable(),
                resultVo.isAutoRefund(),
                resultVo.getApplyStatus());
        return R.ok(resultVo);
    }

    /**
     * 用户查询退款进度（推荐）
     */
    @GetMapping("/refund/progress")
    @Operation(summary = "查询退款进度", description = "按订单号查询最新退款进度，或按退款申请ID精确查询")
    public R<GetRefundProgressVo> getRefundProgress(
            @Valid
            @ModelAttribute
            @Parameter(description = "退款进度查询参数")
            GetRefundProgressRequestDto dto,
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        return R.ok(orderRefundService.getRefundProgress(dto, userId));
    }

    /**
     * 用户按 refundApplyId 查询退款进度（明确路径）
     */
    @GetMapping("/refund/{refundApplyId}")
    @Operation(summary = "按退款申请ID查询退款进度", description = "通过退款申请ID精确查询退款处理进度（默认返回时间线）")
    public R<GetRefundProgressVo> getRefundProgressByApplyId(
            @PathVariable("refundApplyId")
            @NotNull(message = "订单号不能为空")
            @Parameter(description = "退款申请ID", required = true, example = "10086")
            Long refundApplyId,
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {

        GetRefundProgressRequestDto dto = GetRefundProgressRequestDto.builder()
                .refundApplyId(refundApplyId)
                .build();

        return R.ok(orderRefundService.getRefundProgress(dto, userId));
    }

    /**
     * 用户取消退款申请
     * TODO 暂不开放
     */
//    @PostMapping("/refund/cancel")
//    @Operation(summary = "取消退款申请", description = "用户在退款未处理完成前主动取消退款申请")
    public R<CancelRefundApplyResultVo> cancelRefundApply(
            @Valid
            @RequestBody
            @Parameter(description = "取消退款申请参数", required = true)
            CancelRefundApplyRequestDto dto) {

        CancelRefundApplyResultVo vo = orderRefundService.cancelRefundApply(dto);
        return R.ok(vo);
    }
}
