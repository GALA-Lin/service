package com.unlimited.sports.globox.payment.consumer;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.message.order.UserRefundMessage;
import com.unlimited.sports.globox.common.result.PaymentsCode;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.payment.service.AlipayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 用户退款事件 消费者
 */
@Slf4j
@Component
public class UserRefundConsumer {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private JsonUtils jsonUtils;

    @RabbitListener(queues = OrderMQConstants.QUEUE_ORDER_REFUND_APPLY_TO_PAYMENT_PAYMENT)
    @RabbitRetryable(
            finalExchange = OrderMQConstants.EXCHANGE_ORDER_REFUND_APPLY_TO_PAYMENT_FINAL_DLX,
            finalRoutingKey = OrderMQConstants.ROUTING_ORDER_REFUND_APPLY_TO_PAYMENT_FINAL)
    public void onMessage(
            UserRefundMessage message,
            Channel channel,
            Message amqpMessage){
        log.info("开始退款：{}", jsonUtils.objectToJson(message));
        boolean success = alipayService.refund(message);

        if (success) {
            log.info("退款流程执行成功:{}", jsonUtils.objectToJson(message));
        } else {
            log.error("退款流程执行失败:{}", jsonUtils.objectToJson(message));
            throw new GloboxApplicationException(PaymentsCode.PAYMENT_REFUND_FAILED);
        }
    }
}
