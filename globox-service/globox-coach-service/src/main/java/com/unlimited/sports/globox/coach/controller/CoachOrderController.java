package com.unlimited.sports.globox.coach.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_USER_ID;
import com.unlimited.sports.globox.dubbo.order.OrderForCoachDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * @since 2026/1/8 18:46
 * 教练订单管理接口
 */
@Slf4j
@RestController
@RequestMapping("/coach/order")
public class CoachOrderController {

    @DubboReference(group = "rpc", timeout = 10000)
    private OrderForCoachDubboService orderForCoachDubboService;

    /**
     * 获取教练订单分页列表
     *
     * @param coachUserId 教练用户ID（从请求头获取）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页大小（默认10）
     * @return 分页订单列表
     */
    @GetMapping
    public R<IPage<CoachGetOrderResultDto>> getOrderPage(
            @RequestHeader(HEADER_USER_ID) Long coachUserId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Integer pageNum,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于等于1") Integer pageSize) {

        log.info("获取教练订单列表 - coachUserId: {}, pageNum: {}, pageSize: {}",
                coachUserId, pageNum, pageSize);

        // 构建请求参数
        CoachGetOrderPageRequestDto requestDto = CoachGetOrderPageRequestDto.builder()
                .coachId(coachUserId)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();

        // 调用订单服务RPC接口
        RpcResult<IPage<CoachGetOrderResultDto>> rpcResult =
                orderForCoachDubboService.getOrderPage(requestDto);

        // 检查RPC调用结果
        Assert.rpcResultOk(rpcResult);

        log.info("成功获取教练订单列表 - coachUserId: {}, 总记录数: {}",
                coachUserId, rpcResult.getData().getTotal());

        return R.ok(rpcResult.getData());
    }

    /**
     * 获取订单详情
     *
     * @param orderNo 订单号
     * @param coachUserId 教练用户ID（从请求头获取）
     * @return 订单详情
     */
    @GetMapping("/{orderNo}")
    public R<CoachGetOrderResultDto> getOrderDetails(
            @PathVariable @NotNull(message = "订单号不能为空") Long orderNo,
            @RequestHeader(HEADER_USER_ID) Long coachUserId) {

        log.info("获取订单详情 - orderNo: {}, coachUserId: {}", orderNo, coachUserId);

        // 构建请求参数
        CoachGetOrderDetailsRequestDto requestDto = CoachGetOrderDetailsRequestDto.builder()
                .orderNo(orderNo)
                .coachId(coachUserId)
                .build();

        // 调用订单服务RPC接口
        RpcResult<CoachGetOrderResultDto> rpcResult =
                orderForCoachDubboService.getOrderDetails(requestDto);

        // 检查RPC调用结果
        Assert.rpcResultOk(rpcResult);

        log.info("成功获取订单详情 - orderNo: {}, 订单状态: {}",
                orderNo, rpcResult.getData().getOrderStatus());

        return R.ok(rpcResult.getData());
    }

    /**
     * 教练取消未支付订单
     *
     * @param orderNo 订单号
     * @param coachUserId 教练用户ID（从请求头获取）
     * @return 取消结果
     */
    @PostMapping("/{orderNo}/cancel")
    public R<SellerCancelOrderResultDto> cancelUnpaidOrder(
            @PathVariable @NotNull(message = "订单号不能为空") Long orderNo,
            @RequestHeader(HEADER_USER_ID) Long coachUserId) {

        log.info("教练取消未支付订单 - orderNo: {}, coachUserId: {}", orderNo, coachUserId);

        // 构建请求参数
        CoachCancelOrderRequestDto requestDto = CoachCancelOrderRequestDto.builder()
                .orderNo(orderNo)
                .coachId(coachUserId)
                .build();

        // 调用订单服务RPC接口
        RpcResult<SellerCancelOrderResultDto> rpcResult =
                orderForCoachDubboService.cancelUnpaidOrder(requestDto);

        // 检查RPC调用结果
        Assert.rpcResultOk(rpcResult);

        log.info("教练成功取消订单 - orderNo: {}, 取消时间: {}",
                orderNo, rpcResult.getData().getCancelledAt());

        return R.ok(rpcResult.getData());
    }

    /**
     * 教练确认订单
     *
     * @param orderNo 订单号
     * @param coachUserId 教练用户ID（从请求头获取）
     * @return 确认结果
     */
    @PostMapping("/{orderNo}/confirm")
    public R<SellerConfirmResultDto> confirmOrder(
            @PathVariable @NotNull(message = "订单号不能为空") Long orderNo,
            @RequestHeader(HEADER_USER_ID) Long coachUserId) {

        log.info("教练确认订单 - orderNo: {}, coachUserId: {}", orderNo, coachUserId);

        // 构建请求参数
        CoachConfirmRequestDto requestDto = CoachConfirmRequestDto.builder()
                .orderNo(orderNo)
                .isAutoConfirm(false)
                .coachId(coachUserId)
                .build();

        // 调用订单服务RPC接口
        RpcResult<SellerConfirmResultDto> rpcResult =
                orderForCoachDubboService.confirm(requestDto);

        // 检查RPC调用结果
        Assert.rpcResultOk(rpcResult);

        log.info("教练成功确认订单 - orderNo: {}, 确认时间: {}",
                orderNo, rpcResult.getData().getConfirmAt());

        return R.ok(rpcResult.getData());
    }

    /**
     * 教练同意退款
     *
     * @param orderNo 订单号
     * @param dto 退款审批请求参数
     * @param coachUserId 教练用户ID（从请求头获取）
     * @return 审批结果
     */
    @PostMapping("/{orderNo}/approve-refund")
    public R<SellerApproveRefundResultDto> approveRefund(
            @PathVariable @NotNull(message = "订单号不能为空") Long orderNo,
            @Valid @RequestBody CoachApproveRefundRequestDto dto,
            @RequestHeader(HEADER_USER_ID) Long coachUserId) {

        log.info("教练同意退款 - orderNo: {}, coachUserId: {}, refundApplyId: {}, refundPercentage: {}",
                orderNo, coachUserId, dto.getRefundApplyId(), dto.getRefundPercentage());

        // 设置教练ID和订单号
        dto.setCoachId(coachUserId);
        dto.setOrderNo(orderNo);

        // 调用订单服务RPC接口
        RpcResult<SellerApproveRefundResultDto> rpcResult =
                orderForCoachDubboService.approveRefund(dto);

        // 检查RPC调用结果
        Assert.rpcResultOk(rpcResult);

        log.info("教练成功同意退款 - orderNo: {}, 同意退款数量: {}",
                orderNo, rpcResult.getData().getApprovedItemCount());

        return R.ok(rpcResult.getData());
    }

    /**
     * 教练拒绝退款
     *
     * @param orderNo 订单号
     * @param dto 拒绝退款请求参数
     * @param coachUserId 教练用户ID（从请求头获取）
     * @return 拒绝结果
     */
    @PostMapping("/{orderNo}/reject-refund")
    public R<SellerRejectRefundResultDto> rejectRefund(
            @PathVariable @NotNull(message = "订单号不能为空") Long orderNo,
            @Valid @RequestBody CoachRejectRefundRequestDto dto,
            @RequestHeader(HEADER_USER_ID) Long coachUserId) {

        log.info("教练拒绝退款 - orderNo: {}, coachUserId: {}, refundApplyId: {}, remark: {}",
                orderNo, coachUserId, dto.getRefundApplyId(), dto.getRemark());

        // 设置教练ID和订单号
        dto.setCoachId(coachUserId);
        dto.setOrderNo(orderNo);

        // 调用订单服务RPC接口
        RpcResult<SellerRejectRefundResultDto> rpcResult =
                orderForCoachDubboService.rejectRefund(dto);

        // 检查RPC调用结果
        Assert.rpcResultOk(rpcResult);

        log.info("教练成功拒绝退款 - orderNo: {}, 拒绝数量: {}",
                orderNo, rpcResult.getData().getRejectedItemCount());

        return R.ok(rpcResult.getData());
    }

    /**
     * 教练申请退款
     *
     * @param orderNo 订单号
     * @param dto 退款申请请求参数
     * @param coachUserId 教练用户ID（从请求头获取）
     * @return 退款结果
     */
    @PostMapping("/{orderNo}/refund")
    public R<SellerRefundResultDto> refund(
            @PathVariable @NotNull(message = "订单号不能为空") Long orderNo,
            @Valid @RequestBody CoachRefundRequestDto dto,
            @RequestHeader(HEADER_USER_ID) Long coachUserId) {

        log.info("教练申请退款 - orderNo: {}, coachUserId: {}, remark: {}",
                orderNo, coachUserId, dto.getRemark());

        // 设置教练ID和订单号
        dto.setCoachId(coachUserId);
        dto.setOrderNo(orderNo);

        // 调用订单服务RPC接口
        RpcResult<SellerRefundResultDto> rpcResult =
                orderForCoachDubboService.refund(dto);

        // 检查RPC调用结果
        Assert.rpcResultOk(rpcResult);

        log.info("教练成功申请退款 - orderNo: {}, 订单状态: {}",
                orderNo, rpcResult.getData().getOrderStatus());

        return R.ok(rpcResult.getData());
    }
}
