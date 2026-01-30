package com.unlimited.sports.globox.coach.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.unlimited.sports.globox.coach.constants.CoachConstants;
import com.unlimited.sports.globox.coach.mapper.CoachSlotRecordMapper;
import com.unlimited.sports.globox.coach.util.CoachNotificationUtil;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_USER_ID;
import com.unlimited.sports.globox.dubbo.order.OrderForCoachDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.*;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoRequest;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoResponse;
import com.unlimited.sports.globox.dubbo.user.dto.UserPhoneDto;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.model.coach.entity.CoachSlotRecord;
import com.unlimited.sports.globox.model.coach.vo.CoachOrderDetailWithUserInfoVo;
import com.unlimited.sports.globox.model.coach.vo.CoachRecordDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    @DubboReference(group = "rpc", timeout = CoachConstants.DUBBO_RPC_TIMEOUT)
    private OrderForCoachDubboService orderForCoachDubboService;

    @DubboReference(group = "rpc", timeout = CoachConstants.DUBBO_RPC_TIMEOUT)
    private UserDubboService userDubboService;

    @Autowired
    private CoachSlotRecordMapper coachSlotRecordMapper;



    @Autowired
    private CoachNotificationUtil coachNotificationUtil;

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

        // 1. 调用订单服务RPC接口获取分页基础数据
        CoachGetOrderPageRequestDto requestDto = CoachGetOrderPageRequestDto.builder()
                .coachId(coachUserId)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();

        RpcResult<IPage<CoachGetOrderResultDto>> rpcResult =
                orderForCoachDubboService.getOrderPage(requestDto);

        Assert.rpcResultOk(rpcResult);
        IPage<CoachGetOrderResultDto> orderPage = rpcResult.getData();

        if (orderPage.getRecords().isEmpty()) {
            return R.ok(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());
        }

        // 2. 提取并去重所有关联 ID (用于批量查询优化性能)
        // 提取用户ID
        List<Long> userIds = orderPage.getRecords().stream()
                .map(CoachGetOrderResultDto::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 提取所有时段记录ID (RecordId)
        List<Long> allRecordIds = orderPage.getRecords().stream()
                .flatMap(order -> order.getRecords().stream())
                .map(RecordDto::getRecordId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 批量查询本地数据库 (获取场馆和备注)
        Map<Long, CoachSlotRecord> recordMap = Map.of();
        if (!allRecordIds.isEmpty()) {
            List<CoachSlotRecord> slotRecords = coachSlotRecordMapper.selectBatchIds(allRecordIds);
            recordMap = slotRecords.stream()
                    .collect(Collectors.toMap(
                            CoachSlotRecord::getCoachSlotRecordId,
                            record -> record,
                            (v1, v2) -> v1
                    ));
        }

        // 4. 批量调用用户服务 RPC (获取用户信息和手机号)
        // 获取基本资料
        BatchUserInfoRequest userReq = new BatchUserInfoRequest();
        userReq.setUserIds(userIds);
        RpcResult<BatchUserInfoResponse> userRpcResp = userDubboService.batchGetUserInfo(userReq);
        Assert.rpcResultOk(userRpcResp);
        Map<Long, UserInfoVo> userMap = userRpcResp.getData().getUsers().stream()
                .collect(Collectors.toMap(UserInfoVo::getUserId, vo -> vo, (v1, v2) -> v1));

        // 获取手机号
        RpcResult<List<UserPhoneDto>> phoneRpcResult = userDubboService.batchGetUserPhone(userIds);
        Assert.rpcResultOk(phoneRpcResult);
        Map<Long, String> phoneMap = phoneRpcResult.getData().stream()
                .collect(Collectors.toMap(UserPhoneDto::getUserId, UserPhoneDto::getPhone, (v1, v2) -> v1));

        // 5. 转换并封装最终的分页 VO
        Map<Long, CoachSlotRecord> finalRecordMap = recordMap; // 用于 Lambda
        IPage<CoachOrderDetailWithUserInfoVo> resultPage = orderPage.convert(orderDto -> {
            CoachOrderDetailWithUserInfoVo vo = new CoachOrderDetailWithUserInfoVo();
            // 复制基础属性 (orderNo, totalPrice, status 等)
            BeanUtils.copyProperties(orderDto, vo);

            // A. 设置下单人信息
            vo.setBuyerInfo(userMap.get(orderDto.getUserId()));
            vo.setBuyerPhone(phoneMap.get(orderDto.getUserId()));

            // B. 设置时段记录列表 (使用已有的私有转换方法)
            vo.setRecords(convertToCoachRecordDtos(orderDto.getRecords()));

            // C. 提取场馆和备注 (逻辑：取该订单下第一个有时段信息的记录)
            if (orderDto.getRecords() != null) {
                for (RecordDto recordDto : orderDto.getRecords()) {
                    CoachSlotRecord dbRecord = finalRecordMap.get(recordDto.getRecordId());
                    if (dbRecord != null) {
                        // 只要找到一个有值的就填充并跳出（通常同一订单的场地和备注是一致的）
                        if (dbRecord.getVenue() != null) vo.setVenue(dbRecord.getVenue());
                        if (dbRecord.getRemark() != null) vo.setRemark(dbRecord.getRemark());
                        if (vo.getVenue() != null || vo.getRemark() != null) break;
                    }
                }
            }

            return vo;
        });

        log.info("成功处理教练订单列表分页 - 记录数: {}", resultPage.getRecords().size());
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
        // 2. 提取所有 recordId
        List<Long> recordIds = orderDto.getRecords().stream()
                .map(RecordDto::getRecordId)
                .toList();
        String venueName = null;
        String remark = null;

        if (!recordIds.isEmpty()) {
            // 批量查询 coach_slot_record
            List<CoachSlotRecord> slotRecords =
                    coachSlotRecordMapper.selectBatchIds(recordIds);

            // 取第一个非空的 venue/remark（同一订单应该一致）
            CoachSlotRecord firstRecord = slotRecords.stream()
                    .filter(record -> record.getVenue() != null ||
                            record.getRemark() != null)
                    .findFirst()
                    .orElse(!slotRecords.isEmpty() ? slotRecords.get(0) : null);

            if (firstRecord != null) {
                venueName = firstRecord.getVenue();
                remark = firstRecord.getRemark();
            }
        }

        log.info("成功获取订单详情 - orderNo: {}, 订单状态: {}",
                orderNo, orderRpcResult.getData().getOrderStatus());


        RpcResult<UserInfoVo> RpcUserInfo = userDubboService.getUserInfo(orderDto.getUserId());
        Assert.rpcResultOk(RpcUserInfo);
        UserInfoVo userInfoVo = RpcUserInfo.getData();

        RpcResult<UserPhoneDto> RpcUserPhone = userDubboService.getUserPhone(orderDto.getUserId());
        Assert.rpcResultOk(RpcUserPhone);
        String buyerPhone = RpcUserPhone.getData().getPhone();

        // 5. 构建返回VO
        CoachOrderDetailWithUserInfoVo result = CoachOrderDetailWithUserInfoVo.builder()
                .orderNo(orderDto.getOrderNo())
                .userId(orderDto.getUserId())
                .coachId(orderDto.getCoachId())
                .coachName(orderDto.getCoachName())
                .isActivity(orderDto.isActivity())
                .activityTypeName(orderDto.getActivityTypeName())
                .basePrice(orderDto.getBasePrice())
                .extraChargeTotal(orderDto.getExtraChargeTotal())
                .subtotal(orderDto.getSubtotal())
                .discountAmount(orderDto.getDiscountAmount())
                .totalPrice(orderDto.getTotalPrice())
                .paymentStatus(orderDto.getPaymentStatus())
                .paymentStatusName(orderDto.getPaymentStatusName())
                .orderStatus(orderDto.getOrderStatus())
                .orderStatusName(orderDto.getOrderStatusName())
                .source(orderDto.getSource())
                .paidAt(orderDto.getPaidAt())
                .createdAt(orderDto.getCreatedAt())
                .refundApplyId(orderDto.getRefundApplyId())
                // 设置 venue 和 remark（从本地 coach_slot_record 查询）
                .venue(venueName)
                .remark(remark)
                .buyerInfo(userInfoVo)
                .buyerPhone(buyerPhone)
                .records(convertToCoachRecordDtos(orderDto.getRecords()))
                .build();

        log.info("成功查询教练订单详情 - orderNo: {}, venue: {}, remark: {}",
                orderDto.getOrderNo(), venueName, remark);
        log.info("成功获取订单详情 - orderNo: {}, 下单人昵称: {}",
                orderNo, userInfoVo != null ? userInfoVo.getNickName() : "未知");

        return R.ok(result);
    }
    private List<CoachRecordDto> convertToCoachRecordDtos(List<RecordDto> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }

        return records.stream()
                .map(record -> {
                    CoachRecordDto dto = new CoachRecordDto();
                    BeanUtils.copyProperties(record, dto);
                    return dto;
                })
                .collect(Collectors.toList());
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

        // 发送通知给学员
        coachNotificationUtil.handleCoachCancelUnpaidOrderNotification(orderNo, coachUserId);

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

        // 发送确认通知给学员并发送课程提醒延迟消息
        coachNotificationUtil.handleCoachConfirmOrderNotification(orderNo, coachUserId);

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

        // 发送退款批准通知给学员
        coachNotificationUtil.handleCoachApproveRefundNotification(orderNo, coachUserId);

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

        // 发送退款拒绝通知给学员
        coachNotificationUtil.handleCoachRejectRefundNotification(orderNo, coachUserId, dto.getRemark());

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

        // 发送订单被教练取消通知给学员
        coachNotificationUtil.handleCoachRefundNotification(orderNo, coachUserId);

        return R.ok(rpcResult.getData());
    }
}
