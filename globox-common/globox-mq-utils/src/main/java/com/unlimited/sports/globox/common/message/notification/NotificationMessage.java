package com.unlimited.sports.globox.common.message.notification;

import com.alibaba.fastjson2.annotation.JSONField;
import com.unlimited.sports.globox.common.enums.notification.NotificationEntityTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 通知消息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息ID，用于去重
     */
    @JSONField(name = "messageId")
    private String messageId;

    /**
     * 消息类型（三级分类编码）
     * 格式：模块.角色.事件 (MODULE.ROLE.EVENT)
     * 对应 notification_templates.template_code
     * 如：VENUE_BOOKING.VENUE_BOOKER.ORDER_CONFIRMED, COACH_BOOKING.COACH_PROVIDER.NEW_APPOINTMENT等
     */
    @JSONField(name = "messageType")
    private String messageType;

    /**
     * 消息时间戳（毫秒）
     */
    @JSONField(name = "timestamp")
    private Long timestamp;

    /**
     * 发送方系统（来源服务）
     * 如：order-service, im-service, payment-service等
     * 用于追溯消息来源和问题排查
     */
    @JSONField(name = "sourceSystem")
    private String sourceSystem;

    /**
     * 链路追踪ID
     * 用于分布式系统全链路追踪，串联整个调用链
     * 如：使用Spring Cloud Sleuth、SkyWalking等生成
     */
    @JSONField(name = "traceId")
    private String traceId;

    /**
     * 接收者列表
     */
    @JSONField(name = "recipients")
    private List<Recipient> recipients;

    /**
     * 推送内容
     */
    @JSONField(name = "payload")
    private NotificationPayload payload;

    /**
     * 接收者信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Recipient implements Serializable {
        /**
         * 用户ID
         */
        @JSONField(name = "userId")
        private Long userId;

        /**
         * 用户类型：消费者、商家、教练等基础用户分类
         * 用于初步筛选，具体的角色信息由messageType中的二级分类确定
         */
        @JSONField(name = "userType")
        private String userType;
    }

    /**
     * 推送内容
     * 使用模板模式：根据messageType查询模板，用customData替换模板变量
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationPayload implements Serializable {
        /**
         * 业务ID，用于记录和查询
         */
        @JSONField(name = "businessId")
        private Long businessId;

        /**
         * 模板变量（用于替换模板中的{变量}）
         * 如：{orderId: "123456", amount: "199.00", completedAt: "2025-12-30"}
         * 对应模板："您的订单{orderId}已确认，金额{amount}元"
         */
        @JSONField(name = "customData")
        private Map<String, Object> customData;

        /**
         * 附加实体类型（需要附加展示的实体类型）
         * NONE=无附加信息, USER=用户信息...
         */
        @JSONField(name = "attachedEntityType")
        @Builder.Default
        private NotificationEntityTypeEnum attachedEntityType = NotificationEntityTypeEnum.NONE;

        /**
         * 附加实体ID（需要查询的实体ID）
         * 当 attachedEntityType=USER 时，此字段存放用户ID
         * ...
         */
        @JSONField(name = "attachedEntityId")
        private Long attachedEntityId;
    }

    /**
     * 生成唯一的messageId
     * 格式：{eventType}_{userId}_{timestamp}
     *
     * @param eventType 事件类型（如：ORDER_CONFIRMED, ACCOUNT_LOGIN_ELSEWHERE）
     * @param userId 用户ID
     * @return 唯一messageId
     */
    public static String generateMessageId(String eventType, Long userId) {
        return eventType + "_" + userId + "_" + System.currentTimeMillis();
    }

    /**
     * 生成唯一的messageId（批量场景）
     * 格式：{eventType}_BATCH_{timestamp}
     *
     * @param eventType 事件类型
     * @return 唯一messageId
     */
    public static String generateBatchMessageId(String eventType) {
        return eventType + "_BATCH_" + System.currentTimeMillis();
    }
}
