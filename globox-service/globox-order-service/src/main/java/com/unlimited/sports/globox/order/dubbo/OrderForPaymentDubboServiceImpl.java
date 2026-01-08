package com.unlimited.sports.globox.order.dubbo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import com.unlimited.sports.globox.common.enums.order.OrdersPaymentStatusEnum;
import com.unlimited.sports.globox.common.enums.order.SellerTypeEnum;
import com.unlimited.sports.globox.common.result.OrderCode;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.order.OrderForPaymentDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.PaymentGetOrderResultDto;
import com.unlimited.sports.globox.model.order.entity.OrderActivities;
import com.unlimited.sports.globox.model.order.entity.Orders;
import com.unlimited.sports.globox.order.constants.RedisConsts;
import com.unlimited.sports.globox.order.lock.RedisLock;
import com.unlimited.sports.globox.order.mapper.OrderActivitiesMapper;
import com.unlimited.sports.globox.order.mapper.OrdersMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单服务对支付服务提供 rpc 接口
 */
@Component
@DubboService(group = "rpc")
public class OrderForPaymentDubboServiceImpl implements OrderForPaymentDubboService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderActivitiesMapper orderActivitiesMapper;

    /**
     * payment 支付前查询订单情况
     *
     * @param orderNo 订单编号
     * @return 订单相关信息
     */
    @Override
    @RedisLock(value = "#orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    public RpcResult<PaymentGetOrderResultDto> paymentGetOrders(Long orderNo) {

        Orders orders = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo));

        if (!orders.getOrderStatus().equals(OrderStatusEnum.PENDING)) {
            return RpcResult.error(OrderCode.ORDER_CURRENT_NOT_ALLOW_PAY);
        }
        if (!orders.getPaymentStatus().equals(OrdersPaymentStatusEnum.UNPAID)) {
            return RpcResult.error(OrderCode.ORDER_CURRENT_NOT_ALLOW_PAY);
        }

        boolean isActivity = orderActivitiesMapper.exists(Wrappers.<OrderActivities>lambdaQuery()
                .eq(OrderActivities::getOrderNo, orderNo));

        StringBuilder subjectBuilder = new StringBuilder();
        if (orders.getSellerType().equals(SellerTypeEnum.VENUE)) {
            subjectBuilder.append("用户订场：");
        } else if (orders.getSellerType().equals(SellerTypeEnum.COACH)) {
            subjectBuilder.append("用户预约教练：");
        }
        subjectBuilder.append(orders.getSellerName());

        PaymentGetOrderResultDto resultDto = PaymentGetOrderResultDto.builder()
                .orderNo(orderNo)
                .totalAmount(orders.getPayAmount())
                .subject(subjectBuilder.toString())
                .userId(orders.getBuyerId())
                .isActivity(isActivity)
                .build();
        return RpcResult.ok(resultDto);
    }

}
