package com.unlimited.sports.globox.order.dubbo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import com.unlimited.sports.globox.common.enums.order.RefundStatusEnum;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.dubbo.order.OrderForCoachDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.CoachGetOrderPageRequestDto;
import com.unlimited.sports.globox.dubbo.order.dto.CoachGetOrderResultDto;
import com.unlimited.sports.globox.dubbo.order.dto.MerchantGetOrderResultDto;
import com.unlimited.sports.globox.dubbo.order.dto.RecordDto;
import com.unlimited.sports.globox.model.order.entity.OrderActivities;
import com.unlimited.sports.globox.model.order.entity.OrderItems;
import com.unlimited.sports.globox.model.order.entity.Orders;
import com.unlimited.sports.globox.order.mapper.OrderActivitiesMapper;
import com.unlimited.sports.globox.order.mapper.OrderItemsMapper;
import com.unlimited.sports.globox.order.mapper.OrderStatusLogsMapper;
import com.unlimited.sports.globox.order.mapper.OrdersMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                                        .courtId(item.getResourceId())
                                        .courtName(item.getResourceName())
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
                            .coach(order.getSellerId())
                            .coachName(order.getSellerName())
                            .basePrice(order.getBaseAmount())
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
