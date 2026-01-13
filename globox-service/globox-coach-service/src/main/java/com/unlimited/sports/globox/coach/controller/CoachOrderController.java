package com.unlimited.sports.globox.coach.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_USER_ID;
import com.unlimited.sports.globox.dubbo.order.OrderForCoachDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.*;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoRequest;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoResponse;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.model.coach.vo.CoachOrderDetailWithUserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @DubboReference(group = "rpc", timeout = 10000)
    private UserDubboService userDubboService;

    /**
     * 获取教练订单分页列表
     *
     * @param coachUserId 教练用户ID（从请求头获取）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页大小（默认10）
     * @return 分页订单列表
     */
    @GetMapping
    public R<IPage<CoachOrderDetailWithUserInfoVo>> getOrderPage(
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
        IPage<CoachGetOrderResultDto> orderPage = rpcResult.getData();
        // 2. 提取所有 userId 并去重
        List<Long> userIds = orderPage.getRecords().stream()
                .map(CoachGetOrderResultDto::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 批量获取用户信息
        BatchUserInfoRequest userReq = new BatchUserInfoRequest();
        userReq.setUserIds(userIds);
        RpcResult<BatchUserInfoResponse> rpcResult1 = userDubboService.batchGetUserInfo(userReq);
        BatchUserInfoResponse userResp = rpcResult1.getData();
        Map<Long, UserInfoVo> userMap = userResp.getUsers().stream()
                .collect(Collectors.toMap(UserInfoVo::getUserId, vo -> vo, (v1, v2) -> v1));

        // 4. 转换并封装 VO
        // 3. 转换并封装 VO
        IPage<CoachOrderDetailWithUserInfoVo> resultPage = orderPage.convert(orderDto -> {
            CoachOrderDetailWithUserInfoVo vo = new CoachOrderDetailWithUserInfoVo();
            // 复制订单基础属性
            BeanUtils.copyProperties(orderDto, vo);

            // 设置用户信息
            vo.setBuyerInfo(userMap.get(orderDto.getUserId()));
            return vo;
        });

        return R.ok(resultPage);
    }

    /**
     * 获取订单详情
     *
     * @param orderNo 订单号
     * @param coachUserId 教练用户ID（从请求头获取）
     * @return 订单详情
     */
    @GetMapping("/{orderNo}")
    public R<CoachOrderDetailWithUserInfoVo> getOrderDetails(
            @PathVariable @NotNull(message = "订单号不能为空") Long orderNo,
            @RequestHeader(HEADER_USER_ID) Long coachUserId) {

        log.info("获取订单详情 - orderNo: {}, coachUserId: {}", orderNo, coachUserId);

        // 1. 构建请求参数并调用订单服务获取基础订单信息
        CoachGetOrderDetailsRequestDto requestDto = CoachGetOrderDetailsRequestDto.builder()
                .orderNo(orderNo)
                .coachId(coachUserId)
                .build();

        RpcResult<CoachGetOrderResultDto> orderRpcResult =
                orderForCoachDubboService.getOrderDetails(requestDto);

        // 检查订单服务RPC调用结果
        Assert.rpcResultOk(orderRpcResult);

        CoachGetOrderResultDto orderDto = orderRpcResult.getData();

        log.info("成功获取订单详情 - orderNo: {}, 订单状态: {}",
                orderNo, orderRpcResult.getData().getOrderStatus());

        // 3. 封装为业务 VO
        CoachOrderDetailWithUserInfoVo vo = new CoachOrderDetailWithUserInfoVo();
        // 复制订单基础属性 (如 orderNo, totalPrice, statusName 等)
        BeanUtils.copyProperties(orderDto, vo);


        RpcResult<UserInfoVo> rpcResult = userDubboService.getUserInfo(orderDto.getUserId());
        UserInfoVo userInfoVo = rpcResult.getData();
        // 设置下单人信息
        vo.setBuyerInfo(userInfoVo);

        log.info("成功获取订单详情 - orderNo: {}, 下单人昵称: {}",
                orderNo, userInfoVo != null ? userInfoVo.getNickName() : "未知");

        return R.ok(vo);
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
