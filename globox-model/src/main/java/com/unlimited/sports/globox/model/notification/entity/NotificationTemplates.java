package com.unlimited.sports.globox.model.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.unlimited.sports.globox.model.base.BaseEntity;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 推送模板表
 * 无需修改代码即可调整文案
 * 后续使用后台操作可以直接新增模版,而不使用硬编码
 */
@TableName(value = "notification_templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplates implements Serializable {


    @TableId(value = "template_id",type = IdType.AUTO)
    private Long templateId;
    /**
     * 模板编码（完整分类编码）
     * 格式：模块.角色.事件
     * 如：VENUE_BOOKING.VENUE_BOOKER.ORDER_CONFIRMED
     */
    private String templateCode;

    /**
     * 模板名称，如场地订单确认通知
     */
    private String templateName;

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
     * 标题模板，如：您的订单{orderId}已确认
     * 支持{变量}替换
     */
    private String titleTemplate;

    /**
     * 内容模板
     * 支持{变量}替换
     */
    private String contentTemplate;

    /**
     * deeplink模板，支持{变量}，如tennis://order/{orderId}
     */
    private String actionTemplate;

    /**
     * 语言：zh(中文)/en(英文)
     */
    @Builder.Default
    private String lang = "zh";

    /**
     * 是否启用：0=禁用，1=启用
     */
    @Builder.Default
    private Boolean isActive = true;


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
