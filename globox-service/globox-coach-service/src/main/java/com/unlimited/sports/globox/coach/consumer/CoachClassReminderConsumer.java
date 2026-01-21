package com.unlimited.sports.globox.coach.consumer;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.CoachMQConstants;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import com.unlimited.sports.globox.common.message.coach.CoachClassReminderMessage;
import com.unlimited.sports.globox.coach.constants.CoachConstants;
import com.unlimited.sports.globox.coach.util.CoachNotificationUtil;
import com.unlimited.sports.globox.dubbo.order.OrderForCoachDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.CoachGetOrderDetailsRequestDto;
import com.unlimited.sports.globox.dubbo.order.dto.CoachGetOrderResultDto;
import com.unlimited.sports.globox.common.result.RpcResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

/**
 * 教练课程提醒消息消费者
 * 消费延迟的课程提醒消息，验证订单有效性后发送通知给教练和学员
 */
@Slf4j
@Component
public class CoachClassReminderConsumer {

    @DubboReference(group = "rpc", timeout = CoachConstants.DUBBO_RPC_TIMEOUT)
    private OrderForCoachDubboService orderForCoachDubboService;


    @Autowired
    private CoachNotificationUtil coachNotificationUtil;

    @RabbitListener(queues = CoachMQConstants.QUEUE_COACH_CLASS_REMINDER_COACH)
    @Transactional(rollbackFor = Exception.class)
    @RabbitRetryable(
            finalExchange = CoachMQConstants.EXCHANGE_COACH_CLASS_REMINDER_FINAL_DLX,
            finalRoutingKey = CoachMQConstants.ROUTING_COACH_CLASS_REMINDER_FINAL
    )
    public void onClassReminder(
            CoachClassReminderMessage message,
            Channel channel,
            Message amqpMessage) {

        Long orderNo = message.getOrderNo();
        Long coachId = message.getCoachId();
        Long buyerId = message.getBuyerId();
        String coachName = message.getCoachName();

        try {
            // 参数验证
            if (ObjectUtils.isEmpty(orderNo)) {
                log.warn("[课程提醒] orderNo为空，跳过处理");
                return;
            }

            // 1. 查询订单详情以验证订单状态
            CoachGetOrderDetailsRequestDto requestDto = CoachGetOrderDetailsRequestDto.builder()
                    .orderNo(orderNo)
                    .coachId(coachId)
                    .build();

            RpcResult<CoachGetOrderResultDto> orderDetailsResult =
                    orderForCoachDubboService.getOrderDetails(requestDto);

            if (!orderDetailsResult.isSuccess() || ObjectUtils.isEmpty(orderDetailsResult.getData())) {
                log.warn("[课程提醒] 无法获取订单详情，跳过处理 - orderNo: {}", orderNo);
                return;
            }

            CoachGetOrderResultDto orderDetail = orderDetailsResult.getData();

            // 2. 检查订单状态：CONFIRMED、REFUND_REJECTED、REFUND_CANCELLED 状态的订单可以发送提醒
            if (!OrderStatusEnum.CONFIRMED.equals(orderDetail.getOrderStatus()) &&
                    !OrderStatusEnum.REFUND_REJECTED.equals(orderDetail.getOrderStatus()) &&
                    !OrderStatusEnum.REFUND_CANCELLED.equals(orderDetail.getOrderStatus())) {
                log.info("[课程提醒] 订单状态不支持提醒，跳过处理 - orderNo: {}, status: {}", orderNo, orderDetail.getOrderStatus());
                return;
            }

            // 3. 发送课程提醒通知
            coachNotificationUtil.sendCoachClassReminderNotification(
                    orderNo,
                    coachId,
                    buyerId,
                    coachName);

            log.info("[课程提醒] 课程提醒通知发送成功 - orderNo: {}, coachId: {}, buyerId: {}", orderNo, coachId, buyerId);

        } catch (Exception e) {
            log.error("[课程提醒] 处理失败 - orderNo: {}", orderNo, e);
            throw e;
        }
    }
}
