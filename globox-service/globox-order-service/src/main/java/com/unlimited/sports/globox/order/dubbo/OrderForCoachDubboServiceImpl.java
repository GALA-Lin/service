package com.unlimited.sports.globox.order.dubbo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import com.unlimited.sports.globox.common.enums.order.RefundStatusEnum;
import com.unlimited.sports.globox.common.enums.order.SellerTypeEnum;
import com.unlimited.sports.globox.common.lock.RedisLock;
import com.unlimited.sports.globox.common.result.OrderCode;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.dubbo.order.OrderForCoachDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.*;
import com.unlimited.sports.globox.model.order.entity.OrderActivities;
import com.unlimited.sports.globox.model.order.entity.OrderItems;
import com.unlimited.sports.globox.model.order.entity.Orders;
import com.unlimited.sports.globox.order.constants.RedisConsts;
import com.unlimited.sports.globox.order.mapper.OrderActivitiesMapper;
import com.unlimited.sports.globox.order.mapper.OrderItemsMapper;
import com.unlimited.sports.globox.order.mapper.OrderStatusLogsMapper;
import com.unlimited.sports.globox.order.mapper.OrdersMapper;
import com.unlimited.sports.globox.order.service.OrderDubboService;
import com.unlimited.sports.globox.order.service.OrderService;
import io.seata.spring.annotation.GlobalTransactional;
import io.seata.tm.api.transaction.Propagation;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 订单服务对教练服务 提供订单相关接口
 */
@Component
@DubboService(group = "rpc")
public class OrderForCoachDubboServiceImpl implements OrderForCoachDubboService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemsMapper orderItemsMapper;

    @Autowired
    private OrderStatusLogsMapper orderStatusLogsMapper;

    @Autowired
    private MQService mqService;

    @Autowired
    private OrderActivitiesMapper orderActivitiesMapper;

    @Autowired
    private OrderDubboService orderDubboService;

    /**
     * 获取教练的订单分页信息。
     *
     * @param dto 请求参数，包含教练ID、页码和每页大小
     * @return 返回一个RpcResult对象，其中包含了分页后的订单信息列表。每个订单信息包括订单号、用户ID、场馆信息、价格明细、支付状态、订单状态等
     */
    @Override
    public RpcResult<IPage<CoachGetOrderResultDto>> getOrderPage(CoachGetOrderPageRequestDto dto) {
        // 1. 分页查询订单主表
        Page<Orders> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        IPage<Orders> orderPage = ordersMapper.selectPage(
                page,
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getSellerId, dto.getCoachId())
                        .orderByDesc(Orders::getId));

        if (orderPage.getRecords().isEmpty()) {
            Page<CoachGetOrderResultDto> empty =
                    new Page<>(dto.getPageNum(), dto.getPageSize(), 0);
            empty.setRecords(Collections.emptyList());
            return RpcResult.ok(empty);
        }

        // 2. 批量查询订单项
        List<Long> orderNos = orderPage.getRecords()
                .stream()
                .map(Orders::getOrderNo)
                .toList();

        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .in(OrderItems::getOrderNo, orderNos)
                        .orderByAsc(OrderItems::getStartTime));

        Map<Long, List<OrderItems>> itemMap = items.stream()
                .collect(Collectors.groupingBy(OrderItems::getOrderNo));

        // 3. 组装返回 DTO
        List<CoachGetOrderResultDto> coachGetOrderResultDtos = orderPage.getRecords()
                .stream()
                .map(order -> {

                    List<OrderItems> orderItems =
                            itemMap.getOrDefault(order.getOrderNo(), Collections.emptyList());

                    List<RecordDto> recordDtos = orderItems.stream()
                            .map(item -> {

                                boolean cancelable = isOrderItemCancelableByCoach(
                                        order.getOrderStatus(), item.getRefundStatus());

                                return RecordDto.builder()
                                        .recordId(item.getRecordId())
                                        .orderNo(item.getOrderNo())
                                        .resourceId(item.getResourceId())
                                        .resourceName(item.getResourceName())
                                        .bookingDate(item.getBookingDate())
                                        .startTime(item.getStartTime())
                                        .endTime(item.getEndTime())
                                        .unitPrice(item.getUnitPrice())
                                        .status(item.getRefundStatus())
                                        .statusName(item.getRefundStatus().getDescription())
                                        .cancelable(cancelable)
                                        .build();
                            })
                            .toList();

                    CoachGetOrderResultDto resultDto = CoachGetOrderResultDto.builder()
                            .orderNo(order.getOrderNo())
                            .userId(order.getBuyerId())
                            .coachId(order.getSellerId())
                            .coachName(order.getSellerName())
                            .basePrice(order.getBaseAmount())
                            .extraChargeTotal(order.getExtraAmount())
                            .subtotal(order.getSubtotal())
                            .discountAmount(order.getDiscountAmount())
                            .refundApplyId(order.getRefundApplyId())
                            .totalPrice(order.getPayAmount())
                            .paymentStatus(order.getPaymentStatus())
                            .paymentStatusName(order.getPaymentStatus().getDescription())
                            .orderStatus(order.getOrderStatus())
                            .orderStatusName(order.getOrderStatus().getDescription())
                            .source(order.getSourcePlatform())
                            .paidAt(order.getPaidAt())
                            .createdAt(order.getCreatedAt())
                            .records(recordDtos)
                            .build();

                    OrderActivities orderActivities = orderActivitiesMapper.selectOne(
                            Wrappers.<OrderActivities>lambdaQuery()
                                    .eq(OrderActivities::getOrderNo, order.getOrderNo()));

                    if (!ObjectUtils.isEmpty(orderActivities)) {
                        resultDto.setActivity(true);
                        resultDto.setActivityTypeName(orderActivities.getActivityTypeName());
                    } else {
                        resultDto.setActivity(false);
                    }

                    return resultDto;
                })
                .toList();

        // 4. 构建分页返回
        Page<CoachGetOrderResultDto> result =
                new Page<>(dto.getPageNum(), dto.getPageSize(), orderPage.getTotal());
        result.setRecords(coachGetOrderResultDtos);

        return RpcResult.ok(result);
    }


    /**
     * 获取指定订单的详细信息。
     *
     * @param dto 请求参数，包含订单号和教练ID
     * @return 返回一个RpcResult对象，其中包含了订单的详细信息。订单详情包括但不限于订单号、用户ID、场馆信息、价格明细（基础价格、额外费用总和、小计、折扣金额、最终总价）、支付状态、订单状态、创建时间及订单时段列表等
     */
    @Override
    public RpcResult<CoachGetOrderResultDto> getOrderDetails(CoachGetOrderDetailsRequestDto dto) {

        Long orderNo = dto.getOrderNo();
        Long coachId = dto.getCoachId();


        // 1. 查询订单主表
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getSellerId, coachId)
                        .last("limit 1"));

        if (ObjectUtils.isEmpty(order)) {
            return RpcResult.error(OrderCode.ORDER_NOT_EXIST);
        }

        // 2. 查询订单项
        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .orderByAsc(OrderItems::getStartTime));
        if (ObjectUtils.isEmpty(items)) {
            return RpcResult.error(OrderCode.ORDER_ITEM_NOT_EXIST);
        }

        // 3. 构建 recordDtos
        List<RecordDto> recordDtos = items.stream()
                .map(item -> {
                    boolean cancelable = switch (order.getOrderStatus()) {
                        case CONFIRMED, COMPLETED, CANCELLED, REFUNDED -> false;
                        default -> true;
                    };

                    if (item.getRefundStatus() != RefundStatusEnum.NONE) {
                        cancelable = false;
                    }

                    return RecordDto.builder()
                            .recordId(item.getRecordId())
                            .orderNo(item.getOrderNo())
                            .resourceId(item.getResourceId())
                            .resourceName(item.getResourceName())
                            .bookingDate(item.getBookingDate())
                            .startTime(item.getStartTime())
                            .endTime(item.getEndTime())
                            .unitPrice(item.getUnitPrice())
                            .status(item.getRefundStatus())
                            .statusName(item.getRefundStatus().name())
                            .cancelable(cancelable)
                            .build();
                })
                .toList();

        // 4. 构建返回 DTO
        CoachGetOrderResultDto resultDto = CoachGetOrderResultDto.builder()
                .orderNo(order.getOrderNo())
                .userId(order.getBuyerId())
                .coachId(order.getSellerId())
                .coachName(order.getSellerName())
                .basePrice(order.getBaseAmount())
                .refundApplyId(order.getRefundApplyId())
                .extraChargeTotal(order.getExtraAmount())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .totalPrice(order.getPayAmount())
                .paymentStatus(order.getPaymentStatus())
                .paymentStatusName(order.getPaymentStatus().getDescription())
                .orderStatus(order.getOrderStatus())
                .orderStatusName(order.getOrderStatus().getDescription())
                .source(order.getSourcePlatform())
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                .records(recordDtos)
                .build();


        OrderActivities orderActivities = orderActivitiesMapper.selectOne(
                Wrappers.<OrderActivities>lambdaQuery()
                        .eq(OrderActivities::getOrderNo, order.getOrderNo()));

        if (!ObjectUtils.isEmpty(orderActivities)) {
            resultDto.setActivity(true);
            resultDto.setActivityTypeName(orderActivities.getActivityTypeName());
        } else {
            resultDto.setActivity(false);
        }

        return RpcResult.ok(resultDto);
    }


    /**
     * 教练取消未支付订单。
     *
     * @param dto 包含订单号、商家ID以及可选的取消原因的请求参数
     * @return 取消订单的结果，包括订单号、当前订单状态、状态描述及取消时间
     */
    @Override
    public RpcResult<SellerCancelOrderResultDto> cancelUnpaidOrder(CoachCancelOrderRequestDto dto) {
        return orderDubboService.sellerCancelUnpaidOrder(dto.getOrderNo(),
                dto.getCoachId(),
                dto.getCoachId(),
                SellerTypeEnum.COACH);
    }


    /**
     * 教练确认订单的方法。
     *
     * @param dto 包含订单号的请求参数
     * @return 返回商家确认订单的结果，包括订单号、是否确认成功、当前订单状态、状态描述以及确认时间
     */
    @Override
    public RpcResult<SellerConfirmResultDto> confirm(CoachConfirmRequestDto dto) {
        return orderDubboService.sellerConfirm(dto.getOrderNo(),
                dto.isAutoConfirm(),
                dto.getCoachId(),
                SellerTypeEnum.COACH);
    }


    /**
     * 教练同意退款的处理方法。
     *
     * @param dto 包含订单号、退款申请ID、场馆ID和商家ID等信息的请求参数
     * @return 返回商家同意退款的结果，包括订单状态、退款申请状态等信息
     */
    @Override
    public RpcResult<SellerApproveRefundResultDto> approveRefund(CoachApproveRefundRequestDto dto) {
        return orderDubboService.sellerApproveRefund(dto.getOrderNo(),
                dto.getCoachId(),
                dto.getRefundApplyId(),
                SellerTypeEnum.COACH,
                dto.getRefundPercentage());
    }


    /**
     * 教练拒绝退款的处理方法。
     *
     * @param dto 包含订单号、退款申请ID、场馆ID和商家ID等信息的请求参数
     * @return 返回商家拒绝退款的结果，包括订单状态、退款申请状态等信息
     */
    @Override
    public RpcResult<SellerRejectRefundResultDto> rejectRefund(CoachRejectRefundRequestDto dto) {
        return orderDubboService.rejectRefund(dto.getOrderNo(),
                dto.getRefundApplyId(),
                dto.getCoachId(),
                dto.getCoachId(),
                SellerTypeEnum.COACH,
                dto.getRemark());
    }


    /**
     * 教练退款处理方法。
     *
     * @param dto 包含订单号、退款申请ID、场馆ID和商家ID等信息的请求参数
     * @return 返回商家退款的结果，包括订单状态、退款申请状态等信息
     */
    @Override
    @GlobalTransactional(
            // 当前全局事务的名称
            name = "coach-refund",
            // 回滚异常
            rollbackFor = Exception.class,
            // 全局锁重试间隔
            lockRetryInterval = 5000,
            // 全局锁重试次数
            lockRetryTimes = 5,
            // 超时时间
            timeoutMills = 30000,
            //事务传播
            propagation = Propagation.REQUIRES_NEW
    )
    @RedisLock(value = "#dto.orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    public RpcResult<SellerRefundResultDto> refund(CoachRefundRequestDto dto) {
        List<OrderItems> orderItems = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, dto.getOrderNo()));
        if (!ObjectUtils.isEmpty(orderItems)) {
            return RpcResult.error(OrderCode.ORDER_ITEM_NOT_EXIST);
        }
        List<Long> reqItemIds = orderItems.stream().map(OrderItems::getId).toList();
        return orderDubboService.refund(dto.getOrderNo(), dto.getCoachId(), dto.getCoachId(), reqItemIds, SellerTypeEnum.COACH, dto.getRemark());
    }


    /**
     * 是否能通过教练取消/退款
     *
     * @param orderStatusEnum           订单当前状态
     * @param orderItemRefundStatusEnum 订单项当前退款状态
     * @return 简练是否可取消/退款
     */
    private boolean isOrderItemCancelableByCoach(OrderStatusEnum orderStatusEnum, RefundStatusEnum orderItemRefundStatusEnum) {
        // 订单已完成 / 已取消 / 已退款 则 不可取消
        if (orderStatusEnum == OrderStatusEnum.REFUNDING
                || orderStatusEnum == OrderStatusEnum.CANCELLED
                || orderStatusEnum == OrderStatusEnum.REFUND_APPLYING
                || orderStatusEnum == OrderStatusEnum.REFUNDED) {
            return false;
        }

        // 订单项已进入退款流程 则 不可取消
        return orderItemRefundStatusEnum == RefundStatusEnum.NONE;
    }
}
