package com.unlimited.sports.globox.model.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.unlimited.sports.globox.common.enums.notification.PushStatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 推送记录表，用于追踪和统计
 */
@TableName(value = "push_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushRecords  implements Serializable {


    @TableId(value = "record_id",type = IdType.AUTO)
    private Long recordId;
    /**
     * MQ消息ID，用于去重
     */
    @TableField("notification_id")
    private String notificationId;

    /**
     * 一级分类（业务模块）：1=预约场地, 2=教练预定, 3=约球, 4=社交, 5=系统
     */
    @TableField("notification_module")
    private Integer notificationModule;

    /**
     * 二级分类（用户角色）：1=消费者, 2=商家, 3=教练, 4=发起人, 5=参与者
     */
    @TableField("user_role")
    private Integer userRole;

    /**
     * 三级分类（业务事件），如ORDER_CREATED, PAYMENT_SUCCESS
     */
    private String eventType;

    /**
     * 业务ID，如订单ID、预约ID、帖子ID
     */
    @TableField("business_id")
    private String businessId;

    /**
     * 接收用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 用户类型：USER=普通用户, COACH=教练, ADMIN=管理员
     */
    @TableField("user_type")
    private String userType;

    /**
     * 设备ID
     */
    @TableField("device_id")
    private String deviceId;

    /**
     * 推送凭证(registrationId)
     */
    @TableField("device_token")
    private String deviceToken;

    /**
     * 推送标题
     */
    private String title;

    /**
     * 推送内容
     */
    private String content;

    /**
     * deeplink，如tennis://order/order_001
     */
    @TableField("action_url")
    private String action;

    /**
     * 自定义扩展数据（JSON格式字符串）
     */
    @TableField("custom_data")
    private String customData;

    /**
     * 附加实体类型编码（对应 NotificationEntityTypeEnum）
     */
    @TableField("attached_entity_type")
    @Builder.Default
    private Integer attachedEntityType = 0;

    /**
     * 附加实体ID（需要查询的实体ID）
     */
    @TableField("attached_entity_id")
    private Long attachedEntityId;

    /**
     * 推送状态：0=待发送, 1=已发送, 2=已送达, 3=失败, 4=已过滤
     */
    private PushStatusEnum status;

    /**
     * 腾讯云任务ID
     */
    @TableField("tencent_task_id")
    private String tencentTaskId;

    /**
     * 腾讯云错误码
     */
    @TableField("error_code")
    private String errorCode;

    /**
     * 错误信息
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 点击时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("clicked_at")
    private LocalDateTime clickedAt;

    /**
     * 是否已读：0=未读，1=已读
     */
    @TableField("is_read")
    private Integer isRead;

    /**
     * 已读时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("read_at")
    private LocalDateTime readAt;

    /**
     * 实际发送时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("send_at")
    private LocalDateTime sentAt;


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("created_at")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
