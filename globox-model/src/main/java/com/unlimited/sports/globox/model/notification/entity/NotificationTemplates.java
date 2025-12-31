package com.unlimited.sports.globox.model.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
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


    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;

    /**
     * 模板编码
     * 如：ORDER_CONFIRMED, PAYMENT_SUCCESS
     */
    @TableField("template_code")
    private String templateCode;

    /**
     * 模板名称，如场地订单确认通知
     */
    @TableField("template_name")
    private String templateName;

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
    @TableField("event_type")
    private String eventType;

    /**
     * 标题模板，如：您的订单{orderId}已确认
     * 支持{变量}替换
     */
    @TableField("title_template")
    private String titleTemplate;

    /**
     * 内容模板
     * 支持{变量}替换
     */
    @TableField("content_template")
    private String contentTemplate;

    /**
     * 跳转类型：deeplink/url
     */
    @TableField("action_type")
    private Integer actionType;

    /**
     * 跳转目标，支持{变量}
     * 如：tennis://order/{orderId}
     */
    @TableField("action_target")
    private String actionTarget;

    /**
     * 语言：zh(中文)/en(英文)
     */
    @Builder.Default
    private String lang = "zh";

    /**
     * 是否启用：0=禁用，1=启用
     */
    @Builder.Default
    @TableField("is_active")
    private Boolean isActive = true;

    /**
     * 逻辑删除：0=未删除, 1=已删除
     */
    @Builder.Default
    private Integer deleted = 0;

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
