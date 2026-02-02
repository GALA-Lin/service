package com.unlimited.sports.globox.order.dubbo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import com.unlimited.sports.globox.common.result.OrderCode;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.order.OrderForUserDubboService;
import com.unlimited.sports.globox.model.order.entity.Orders;
import com.unlimited.sports.globox.order.mapper.OrdersMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单服务对用户服务提供 rpc 接口
 */
@Component
@DubboService(group = "rpc")
public class OrderForUserDubboServiceImpl implements OrderForUserDubboService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Override
    public RpcResult<Void> checkOrderStatusBeforeUserCancel(Long userId) {
        List<OrderStatusEnum> statusList = List.of(
                OrderStatusEnum.PENDING,
                OrderStatusEnum.PAID,
                OrderStatusEnum.CONFIRMED,
                OrderStatusEnum.REFUND_APPLYING,
                OrderStatusEnum.REFUNDING,
                OrderStatusEnum.PARTIALLY_REFUNDED,
                OrderStatusEnum.REFUND_REJECTED,
                OrderStatusEnum.REFUND_CANCELLED
        );
        Long count = ordersMapper.selectCount(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getBuyerId, userId)
                        .in(Orders::getOrderStatus, statusList)
        );

        return count == 0 ? RpcResult.ok() : RpcResult.error(OrderCode.EXIST_UNFINISHED_ORDER);
    }
}
