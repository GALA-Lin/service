package com.unlimited.sports.globox.payment.consumer;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.enums.governance.MQBizTypeEnum;
import com.unlimited.sports.globox.common.message.order.ProfitSharingMessage;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import com.unlimited.sports.globox.payment.service.PaymentsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 分账消费者
 */
@Slf4j
@Component
public class ProfitSharingConsumer {

    @Autowired
    private PaymentsService paymentsService;

    @RabbitListener(queues = OrderMQConstants.QUEUE_ORDER_PROFIT_SHARING_REQUEST_TO_PAYMENT_PAYMENT)
    @RabbitRetryable(
            finalExchange = OrderMQConstants.EXCHANGE_ORDER_PROFIT_SHARING_REQUEST_TO_PAYMENT_FINAL_DLX,
            finalRoutingKey = OrderMQConstants.ROUTING_ORDER_PROFIT_SHARING_REQUEST_TO_PAYMENT_FINAL,
            bizKey = "#message.orderNo",
            bizType = MQBizTypeEnum.PROFIT_SHARING
    )
    public void onMessage(
            ProfitSharingMessage message,
            Channel channel,
            Message amqpMessage) {
        String outTradeNo = message.getOutTradeNo();
        Payments payments = paymentsService.getPaymentByOutTradeNo(outTradeNo);
        paymentsService.profitSharing(payments);
    }

}
