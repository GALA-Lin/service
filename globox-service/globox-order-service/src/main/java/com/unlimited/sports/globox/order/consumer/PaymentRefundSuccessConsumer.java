package com.unlimited.sports.globox.order.consumer;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.PaymentMQConstants;
import com.unlimited.sports.globox.common.enums.governance.MQBizTypeEnum;
import com.unlimited.sports.globox.common.lock.RedisLock;
import com.unlimited.sports.globox.common.message.payment.PaymentRefundMessage;
import com.unlimited.sports.globox.order.constants.RedisConsts;
import com.unlimited.sports.globox.order.service.OrderRefundActionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 退款成功事件 消费者
 */
@Slf4j
@Component
public class PaymentRefundSuccessConsumer {

    @Autowired
    private OrderRefundActionService orderRefundActionService;

    /**
     * 退款成功回调 消费者
     */
    @RabbitListener(queues = PaymentMQConstants.QUEUE_PAYMENT_REFUND_SUCCESS_ORDER)
    @RedisLock(value = "#message.orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    @RabbitRetryable(
            finalExchange = PaymentMQConstants.EXCHANGE_PAYMENT_REFUND_SUCCESS_FINAL_DLX,
            finalRoutingKey = PaymentMQConstants.ROUTING_PAYMENT_REFUND_SUCCESS_FINAL,
            bizKey = "#message.orderNo",
            bizType = MQBizTypeEnum.PAYMENT_REFUND_SUCCESS
    )
    public void onMessage(
            PaymentRefundMessage message,
            Channel channel,
            Message amqpMessage) {

        orderRefundActionService.refundSuccessMQHandler(message);

    }
}
