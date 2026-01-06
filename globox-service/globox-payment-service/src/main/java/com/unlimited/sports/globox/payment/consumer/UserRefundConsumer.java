package com.unlimited.sports.globox.payment.consumer;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.constants.PaymentMQConstants;
import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.message.order.UserRefundMessage;
import com.unlimited.sports.globox.common.message.payment.PaymentRefundMessage;
import com.unlimited.sports.globox.common.result.PaymentsCode;
import com.unlimited.sports.globox.common.service.MQService;
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
    private MQService mqService;

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
        boolean success = false;

        if (PaymentTypeEnum.ALIPAY.equals(message.getPaymentType())) {
            // 支付宝退款实现
            success = alipayService.refund(message);
        } else if (PaymentTypeEnum.WECHAT_PAY.equals(message.getPaymentType())) {
            // TODO 微信支付退款实现

        } else {
            // 不支持的退款方式
            throw new GloboxApplicationException(PaymentsCode.NOT_SUPPORTED_PAYMENT_TYPE);
        }

        if (success) {
            log.info("退款流程执行成功:{}", jsonUtils.objectToJson(message));
            PaymentRefundMessage refundMessage = PaymentRefundMessage.builder()
                    .outRequestNo(message.getOutRequestNo())
                    .orderNo(message.getOrderNo())
                    .build();
            mqService.send(
                    PaymentMQConstants.EXCHANGE_TOPIC_PAYMENT_REFUND_SUCCESS,
                    PaymentMQConstants.ROUTING_PAYMENT_REFUND_SUCCESS,
                    refundMessage);
        } else {
            log.error("退款流程执行失败:{}", jsonUtils.objectToJson(message));
            throw new GloboxApplicationException(PaymentsCode.PAYMENT_REFUND_FAILED);
        }
    }
}
