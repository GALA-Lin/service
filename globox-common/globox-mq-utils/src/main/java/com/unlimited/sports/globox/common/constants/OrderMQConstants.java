package com.unlimited.sports.globox.common.constants;

/**
 * 订单相关 MQ 常量
 */
public class OrderMQConstants {

    /**
     * 订单创建失败或取消 → 通知场地解锁
     * 主交换机 & 主路由 & 主队列
     */
    public static final String EXCHANGE_TOPIC_ORDER_UNLOCK_SLOT =
            "exchange.topic.order.unlock-slot";
    public static final String ROUTING_ORDER_UNLOCK_SLOT =
            "routing.order.unlock-slot";
    public static final String QUEUE_ORDER_UNLOCK_SLOT_MERCHANT =
            "queue.order.unlock-slot.merchant";

    /**
     * 重试队列（TTL）
     */
    public static final String QUEUE_ORDER_UNLOCK_SLOT_MERCHANT_RETRY =
            "queue.order.unlock-slot.merchant.retry";

    /**
     * Retry-DLX：主队列失败后进入重试队列
     */
    public static final String EXCHANGE_ORDER_UNLOCK_SLOT_RETRY_DLX =
            "exchange.topic.order.unlock-slot.retry.dlx";
    public static final String ROUTING_ORDER_UNLOCK_SLOT_RETRY =
            "routing.order.unlock-slot.retry";

    /**
     * Final-DLX：超过最大次数进入最终 DLQ
     */
    public static final String EXCHANGE_ORDER_UNLOCK_SLOT_FINAL_DLX =
            "exchange.topic.order.unlock-slot.final.dlx";
    public static final String ROUTING_ORDER_UNLOCK_SLOT_FINAL =
            "routing.order.unlock-slot.final";
    public static final String QUEUE_ORDER_UNLOCK_SLOT_MERCHANT_DLQ =
            "queue.order.unlock-slot.merchant.dlq";


    /**
     * 订单创建失败或取消 → 通知教练模块解锁槽位（Unlock Coach Slot）
     * 主交换机 & 主路由 & 主队列
     */
    public static final String EXCHANGE_TOPIC_ORDER_UNLOCK_COACH_SLOT =
            "exchange.topic.order.unlock-coach-slot";
    public static final String ROUTING_ORDER_UNLOCK_COACH_SLOT =
            "routing.order.unlock-coach-slot";
    public static final String QUEUE_ORDER_UNLOCK_COACH_SLOT_COACH =
            "queue.order.unlock-coach-slot.coach";

    public static final String QUEUE_ORDER_UNLOCK_COACH_SLOT_COACH_RETRY =
            "queue.order.unlock-coach-slot.coach.retry";
    public static final String EXCHANGE_ORDER_UNLOCK_COACH_SLOT_RETRY_DLX =
            "exchange.topic.order.unlock-coach-slot.retry.dlx";
    public static final String ROUTING_ORDER_UNLOCK_COACH_SLOT_RETRY =
            "routing.order.unlock-coach-slot.retry";

    public static final String EXCHANGE_ORDER_UNLOCK_COACH_SLOT_FINAL_DLX =
            "exchange.topic.order.unlock-coach-slot.final.dlx";
    public static final String ROUTING_ORDER_UNLOCK_COACH_SLOT_FINAL =
            "routing.order.unlock-coach-slot.final";
    public static final String QUEUE_ORDER_UNLOCK_COACH_SLOT_COACH_DLQ =
            "queue.order.unlock-coach-slot.coach.dlq";



    /**
     * 订单未支付自动关闭
     * 主交换机 & 主路由 & 主队列
     */
    public static final String EXCHANGE_TOPIC_ORDER_AUTO_CANCEL =
            "exchange.topic.order.auto-cancel";
    public static final String ROUTING_ORDER_AUTO_CANCEL =
            "routing.order.auto-cancel";
    public static final String QUEUE_ORDER_AUTO_CANCEL_ORDER =
            "queue.order.auto-cancel.order";

    /**
     * 重试队列（TTL）
     */
    public static final String QUEUE_ORDER_AUTO_CANCEL_ORDER_RETRY =
            "queue.order.auto-cancel.order.retry";

    /**
     * Retry-DLX：主队列失败后进入重试队列
     */
    public static final String EXCHANGE_ORDER_AUTO_CANCEL_RETRY_DLX =
            "exchange.topic.order.auto-cancel.retry.dlx";
    public static final String ROUTING_ORDER_AUTO_CANCEL_RETRY =
            "routing.order.auto-cancel.retry";

    /**
     * Final-DLX：超过最大次数进入最终 DLQ
     */
    public static final String EXCHANGE_ORDER_AUTO_CANCEL_FINAL_DLX =
            "exchange.topic.order.auto-cancel.final.dlx";
    public static final String ROUTING_ORDER_AUTO_CANCEL_FINAL =
            "routing.order.auto-cancel.final";
    public static final String QUEUE_ORDER_AUTO_CANCEL_ORDER_DLQ =
            "queue.order.auto-cancel.order.dlq";


    /**
     * 订单定时完成（Auto Complete）
     * 主交换机 & 主路由 & 主队列
     */
    public static final String EXCHANGE_TOPIC_ORDER_AUTO_COMPLETE =
            "exchange.topic.order.auto-complete";
    public static final String ROUTING_ORDER_AUTO_COMPLETE =
            "routing.order.auto-complete";
    public static final String QUEUE_ORDER_AUTO_COMPLETE_ORDER =
            "queue.order.auto-complete.order";

    /**
     * 重试队列（TTL）
     */
    public static final String QUEUE_ORDER_AUTO_COMPLETE_ORDER_RETRY =
            "queue.order.auto-complete.order.retry";

    /**
     * Retry-DLX：主队列失败后进入重试队列
     */
    public static final String EXCHANGE_ORDER_AUTO_COMPLETE_RETRY_DLX =
            "exchange.topic.order.auto-complete.retry.dlx";
    public static final String ROUTING_ORDER_AUTO_COMPLETE_RETRY =
            "routing.order.auto-complete.retry";

    /**
     * Final-DLX：超过最大次数进入最终 DLQ
     */
    public static final String EXCHANGE_ORDER_AUTO_COMPLETE_FINAL_DLX =
            "exchange.topic.order.auto-complete.final.dlx";
    public static final String ROUTING_ORDER_AUTO_COMPLETE_FINAL =
            "routing.order.auto-complete.final";
    public static final String QUEUE_ORDER_AUTO_COMPLETE_ORDER_DLQ =
            "queue.order.auto-complete.order.dlq";



    /**
     * 订单服务 → 通知商家服务确认订单（Confirm Order）
     * 说明：
     * - 主队列：商家服务消费，执行“确认订单”相关逻辑
     * - Retry：短延迟重试（例如并发冲突、锁冲突、依赖服务暂不可用）
     * - Final DLQ：超过最大次数后进入最终死信，人工/补偿处理
     */
    // 主交换机 & 主路由 & 主队列
    public static final String EXCHANGE_TOPIC_ORDER_CONFIRM_NOTIFY_MERCHANT =
            "exchange.topic.order.confirm.notify-merchant";
    public static final String ROUTING_ORDER_CONFIRM_NOTIFY_MERCHANT =
            "routing.order.confirm.notify-merchant";
    public static final String QUEUE_ORDER_CONFIRM_NOTIFY_MERCHANT =
            "queue.order.confirm.notify-merchant.merchant";

    // Retry Queue（TTL）
    public static final String QUEUE_ORDER_CONFIRM_NOTIFY_MERCHANT_RETRY =
            "queue.order.confirm.notify-merchant.merchant.retry";

    // Retry-DLX：主队列失败后进入重试队列
    public static final String EXCHANGE_ORDER_CONFIRM_NOTIFY_MERCHANT_RETRY_DLX =
            "exchange.topic.order.confirm.notify-merchant.retry.dlx";
    public static final String ROUTING_ORDER_CONFIRM_NOTIFY_MERCHANT_RETRY =
            "routing.order.confirm.notify-merchant.retry";

    // Final-DLX：超过最大次数进入最终 DLQ
    public static final String EXCHANGE_ORDER_CONFIRM_NOTIFY_MERCHANT_FINAL_DLX =
            "exchange.topic.order.confirm.notify-merchant.final.dlx";
    public static final String ROUTING_ORDER_CONFIRM_NOTIFY_MERCHANT_FINAL =
            "routing.order.confirm.notify-merchant.final";
    public static final String QUEUE_ORDER_CONFIRM_NOTIFY_MERCHANT_DLQ =
            "queue.order.confirm.notify-merchant.merchant.dlq";


    /**
     * 订单通知商家 订单已变为已支付状态事件
     */
    // 主交换机 & 主路由 & 主队列
    public static final String EXCHANGE_TOPIC_ORDER_PAYMENT_CONFIRMED_NOTIFY_MERCHANT =
            "exchange.topic.order.payment-confirmed.notify-merchant";
    public static final String ROUTING_ORDER_PAYMENT_CONFIRMED_NOTIFY_MERCHANT =
            "routing.order.payment-confirmed.notify-merchant";
    public static final String QUEUE_ORDER_PAYMENT_CONFIRMED_NOTIFY_MERCHANT =
            "queue.order.payment-confirmed.notify-merchant.merchant";

    // Retry Queue（TTL）
    public static final String QUEUE_ORDER_PAYMENT_CONFIRMED_NOTIFY_MERCHANT_RETRY =
            "queue.order.payment-confirmed.notify-merchant.merchant.retry";

    // Retry-DLX：主队列失败后进入重试队列
    public static final String EXCHANGE_ORDER_PAYMENT_CONFIRMED_NOTIFY_MERCHANT_RETRY_DLX =
            "exchange.topic.order.payment-confirmed.notify-merchant.retry.dlx";
    public static final String ROUTING_ORDER_PAYMENT_CONFIRMED_NOTIFY_MERCHANT_RETRY =
            "routing.order.payment-confirmed.notify-merchant.retry";

    // Final-DLX：超过最大次数进入最终 DLQ
    public static final String EXCHANGE_ORDER_PAYMENT_CONFIRMED_NOTIFY_MERCHANT_FINAL_DLX =
            "exchange.topic.order.payment-confirmed.notify-merchant.final.dlx";
    public static final String ROUTING_ORDER_PAYMENT_CONFIRMED_NOTIFY_MERCHANT_FINAL =
            "routing.order.payment-confirmed.notify-merchant.final";
    public static final String QUEUE_ORDER_PAYMENT_CONFIRMED_NOTIFY_MERCHANT_DLQ =
            "queue.order.payment-confirmed.notify-merchant.merchant.dlq";


    /**
     * 订单通知教练：订单已变为已支付状态事件
     */
    // 主交换机 & 主路由 & 主队列
    public static final String EXCHANGE_TOPIC_ORDER_PAYMENT_CONFIRMED_NOTIFY_COACH =
            "exchange.topic.order.payment-confirmed.notify-coach";
    public static final String ROUTING_ORDER_PAYMENT_CONFIRMED_NOTIFY_COACH =
            "routing.order.payment-confirmed.notify-coach";
    public static final String QUEUE_ORDER_PAYMENT_CONFIRMED_NOTIFY_COACH =
            "queue.order.payment-confirmed.notify-coach.coach";

    // Retry Queue（TTL）
    public static final String QUEUE_ORDER_PAYMENT_CONFIRMED_NOTIFY_COACH_RETRY =
            "queue.order.payment-confirmed.notify-coach.coach.retry";

    // Retry-DLX：主队列失败后进入重试队列
    public static final String EXCHANGE_ORDER_PAYMENT_CONFIRMED_NOTIFY_COACH_RETRY_DLX =
            "exchange.topic.order.payment-confirmed.notify-coach.retry.dlx";
    public static final String ROUTING_ORDER_PAYMENT_CONFIRMED_NOTIFY_COACH_RETRY =
            "routing.order.payment-confirmed.notify-coach.retry";

    // Final-DLX：超过最大次数进入最终 DLQ
    public static final String EXCHANGE_ORDER_PAYMENT_CONFIRMED_NOTIFY_COACH_FINAL_DLX =
            "exchange.topic.order.payment-confirmed.notify-coach.final.dlx";
    public static final String ROUTING_ORDER_PAYMENT_CONFIRMED_NOTIFY_COACH_FINAL =
            "routing.order.payment-confirmed.notify-coach.final";
    public static final String QUEUE_ORDER_PAYMENT_CONFIRMED_NOTIFY_COACH_DLQ =
            "queue.order.payment-confirmed.notify-coach.coach.dlq";


    /**
     * 订单 -> 支付：申请退款事件（Refund Apply）
     * 说明：
     * - 主队列：支付模块消费，执行退款处理逻辑
     * - Retry：短延迟重试（并发冲突、锁冲突、依赖服务暂不可用）
     * - Final DLQ：超过最大次数进入最终死信，人工/补偿处理
     */
// 主交换机 & 主路由 & 主队列
    public static final String EXCHANGE_TOPIC_ORDER_REFUND_APPLY_TO_PAYMENT =
            "exchange.topic.order.refund-apply.to-payment";
    public static final String ROUTING_ORDER_REFUND_APPLY_TO_PAYMENT =
            "routing.order.refund-apply.to-payment";
    public static final String QUEUE_ORDER_REFUND_APPLY_TO_PAYMENT_PAYMENT =
            "queue.order.refund-apply.to-payment.payment";

    // Retry Queue（TTL）
    public static final String QUEUE_ORDER_REFUND_APPLY_TO_PAYMENT_PAYMENT_RETRY =
            "queue.order.refund-apply.to-payment.payment.retry";

    // Retry-DLX：主队列失败后进入重试队列
    public static final String EXCHANGE_ORDER_REFUND_APPLY_TO_PAYMENT_RETRY_DLX =
            "exchange.topic.order.refund-apply.to-payment.retry.dlx";
    public static final String ROUTING_ORDER_REFUND_APPLY_TO_PAYMENT_RETRY =
            "routing.order.refund-apply.to-payment.retry";

    // Final-DLX：超过最大次数进入最终 DLQ
    public static final String EXCHANGE_ORDER_REFUND_APPLY_TO_PAYMENT_FINAL_DLX =
            "exchange.topic.order.refund-apply.to-payment.final.dlx";
    public static final String ROUTING_ORDER_REFUND_APPLY_TO_PAYMENT_FINAL =
            "routing.order.refund-apply.to-payment.final";
    public static final String QUEUE_ORDER_REFUND_APPLY_TO_PAYMENT_PAYMENT_DLQ =
            "queue.order.refund-apply.to-payment.payment.dlq";


    /**
     * 订单 -> 支付：请求分账事件（Profit Sharing Request）
     * 说明：
     * - 主队列：支付模块消费，执行分账请求/发起分账逻辑
     * - Retry：短延迟重试（并发冲突、锁冲突、依赖服务暂不可用）
     * - Final DLQ：超过最大次数进入最终死信，人工/补偿处理
     */
    // 主交换机 & 主路由 & 主队列
    public static final String EXCHANGE_TOPIC_ORDER_PROFIT_SHARING_REQUEST_TO_PAYMENT =
            "exchange.topic.order.profit-sharing.request.to-payment";
    public static final String ROUTING_ORDER_PROFIT_SHARING_REQUEST_TO_PAYMENT =
            "routing.order.profit-sharing.request.to-payment";
    public static final String QUEUE_ORDER_PROFIT_SHARING_REQUEST_TO_PAYMENT_PAYMENT =
            "queue.order.profit-sharing.request.to-payment.payment";

    // Retry Queue（TTL）
    public static final String QUEUE_ORDER_PROFIT_SHARING_REQUEST_TO_PAYMENT_PAYMENT_RETRY =
            "queue.order.profit-sharing.request.to-payment.payment.retry";

    // Retry-DLX：主队列失败后进入重试队列
    public static final String EXCHANGE_ORDER_PROFIT_SHARING_REQUEST_TO_PAYMENT_RETRY_DLX =
            "exchange.topic.order.profit-sharing.request.to-payment.retry.dlx";
    public static final String ROUTING_ORDER_PROFIT_SHARING_REQUEST_TO_PAYMENT_RETRY =
            "routing.order.profit-sharing.request.to-payment.retry";

    // Final-DLX：超过最大次数进入最终 DLQ
    public static final String EXCHANGE_ORDER_PROFIT_SHARING_REQUEST_TO_PAYMENT_FINAL_DLX =
            "exchange.topic.order.profit-sharing.request.to-payment.final.dlx";
    public static final String ROUTING_ORDER_PROFIT_SHARING_REQUEST_TO_PAYMENT_FINAL =
            "routing.order.profit-sharing.request.to-payment.final";
    public static final String QUEUE_ORDER_PROFIT_SHARING_REQUEST_TO_PAYMENT_PAYMENT_DLQ =
            "queue.order.profit-sharing.request.to-payment.payment.dlq";

}
