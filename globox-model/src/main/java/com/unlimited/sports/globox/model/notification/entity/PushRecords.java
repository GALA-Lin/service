package com.unlimited.sports.globox.model.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.unlimited.sports.globox.model.notification.enums.PushStatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

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
    private String notificationId;

    /**
     * 一级分类（业务模块）：1=预约场地, 2=教练预定, 3=约球, 4=社交, 5=系统
     */
    private Integer notificationModule;

    /**
     * 二级分类（用户角色）：1=消费者, 2=商家, 3=教练, 4=发起人, 5=参与者
     */
    private Integer userRole;

    /**
     * 三级分类（业务事件），如ORDER_CREATED, PAYMENT_SUCCESS
     */
    private String eventType;

    /**
     * 完整分类编码，格式：模块.角色.事件，如VENUE_BOOKING.CONSUMER.ORDER_CONFIRMED
     */
    private String messageType;

    /**
     * 业务ID，如订单ID、预约ID、帖子ID
     */
    private String businessId;

    /**
     * 接收用户ID
     */
    private Long userId;

    /**
     * 用户类型：1=消费者, 2=商家, 3=教练
     */
    private Integer userType;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 推送凭证(registrationId)
     */
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
    private String action;

    /**
     * 自定义扩展数据（JSON格式）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private java.util.Map<String, Object> customData;

    /**
     * 推送状态：0=待发送, 1=已发送, 2=已送达, 3=失败, 4=已过滤
     */
    private PushStatusEnum status;

    /**
     * 腾讯云任务ID
     */
    private String tencentTaskId;

    /**
     * 腾讯云错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 点击时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime clickedAt;

    /**
     * 实际发送时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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
