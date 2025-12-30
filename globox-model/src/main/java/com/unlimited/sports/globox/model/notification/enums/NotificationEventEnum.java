package com.unlimited.sports.globox.model.notification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知事件枚举（三级分类）
 * 强绑定到模块和角色
 * 编码规则：模块.角色.事件
 */
@Getter
@AllArgsConstructor
public enum NotificationEventEnum {

    /**
     * VENUE_BOOKING 预约场地模块
     */


    // 订场者事件
    VENUE_ORDER_CONFIRMED(NotificationModuleEnum.VENUE_BOOKING, NotificationRoleEnum.VENUE_BOOKER, "VENUE_ORDER_CONFIRMED", "订单已确认"),
    VENUE_ORDER_CANCELLED(NotificationModuleEnum.VENUE_BOOKING, NotificationRoleEnum.VENUE_BOOKER, "VENUE_ORDER_CANCELLED", "订单已取消"),
    VENUE_ORDER_COMPLETED(NotificationModuleEnum.VENUE_BOOKING, NotificationRoleEnum.VENUE_BOOKER, "VENUE_ORDER_COMPLETED", "订单已完成"),
    VENUE_PAYMENT_SUCCESS(NotificationModuleEnum.VENUE_BOOKING, NotificationRoleEnum.VENUE_BOOKER, "VENUE_PAYMENT_SUCCESS", "支付成功"),
    VENUE_REFUND_APPROVED(NotificationModuleEnum.VENUE_BOOKING, NotificationRoleEnum.VENUE_BOOKER, "VENUE_REFUND_APPROVED", "退款申请已通过"),
    VENUE_REFUND_SUCCESS(NotificationModuleEnum.VENUE_BOOKING, NotificationRoleEnum.VENUE_BOOKER, "VENUE_REFUND_SUCCESS", "退款已到账"),

    // 商家事件
    VENUE_MERCHANT_ORDER_CREATED(NotificationModuleEnum.VENUE_BOOKING, NotificationRoleEnum.VENUE_MERCHANT, "VENUE_MERCHANT_ORDER_CREATED", "订单已创建"),
    VENUE_MERCHANT_ORDER_CANCELLED(NotificationModuleEnum.VENUE_BOOKING, NotificationRoleEnum.VENUE_MERCHANT, "VENUE_MERCHANT_ORDER_CANCELLED", "订单已取消"),
    VENUE_MERCHANT_PAYMENT_RECEIVED(NotificationModuleEnum.VENUE_BOOKING, NotificationRoleEnum.VENUE_MERCHANT, "VENUE_MERCHANT_PAYMENT_RECEIVED", "收到款项"),
    VENUE_MERCHANT_REFUND_APPLIED(NotificationModuleEnum.VENUE_BOOKING, NotificationRoleEnum.VENUE_MERCHANT, "VENUE_MERCHANT_REFUND_APPLIED", "收到退款申请"),

    /**
     * COACH_BOOKING 教练预定模块
     */

    // 预约者事件
    COACH_APPOINTMENT_CONFIRMED(NotificationModuleEnum.COACH_BOOKING, NotificationRoleEnum.COACH_BOOKER, "COACH_APPOINTMENT_CONFIRMED", "预约已确认"),
    COACH_APPOINTMENT_CANCELLED(NotificationModuleEnum.COACH_BOOKING, NotificationRoleEnum.COACH_BOOKER, "COACH_APPOINTMENT_CANCELLED", "预约已取消"),
    COACH_PAYMENT_SUCCESS(NotificationModuleEnum.COACH_BOOKING, NotificationRoleEnum.COACH_BOOKER, "COACH_PAYMENT_SUCCESS", "支付成功"),

    // 教练事件
    COACH_PROVIDER_APPOINTMENT_CREATED(NotificationModuleEnum.COACH_BOOKING, NotificationRoleEnum.COACH_PROVIDER, "COACH_PROVIDER_APPOINTMENT_CREATED", "预约已创建"),
    COACH_PROVIDER_APPOINTMENT_CANCELLED(NotificationModuleEnum.COACH_BOOKING, NotificationRoleEnum.COACH_PROVIDER, "COACH_PROVIDER_APPOINTMENT_CANCELLED", "预约已取消"),
    COACH_PROVIDER_PAYMENT_RECEIVED(NotificationModuleEnum.COACH_BOOKING, NotificationRoleEnum.COACH_PROVIDER, "COACH_PROVIDER_PAYMENT_RECEIVED", "收到课程费用"),
    COACH_PROVIDER_REFUND_APPLIED(NotificationModuleEnum.COACH_BOOKING, NotificationRoleEnum.COACH_PROVIDER, "COACH_PROVIDER_REFUND_APPLIED", "收到预约退款申请"),
    COACH_PROVIDER_APPROVAL_PASSED(NotificationModuleEnum.COACH_BOOKING, NotificationRoleEnum.COACH_PROVIDER, "COACH_PROVIDER_APPROVAL_PASSED", "审核已通过"),
    COACH_PROVIDER_APPROVAL_FAILED(NotificationModuleEnum.COACH_BOOKING, NotificationRoleEnum.COACH_PROVIDER, "COACH_PROVIDER_APPROVAL_FAILED", "审核未通过"),
    /**
     * PLAY_MATCHING 约球模块
     */

    // 发起人事件
    RALLY_PARTICIPANT_APPLICATION(NotificationModuleEnum.PLAY_MATCHING, NotificationRoleEnum.RALLY_INITIATOR, "RALLY_PARTICIPANT_APPLICATION", "有人申请加入"),
    RALLY_PARTICIPANT_QUIT(NotificationModuleEnum.PLAY_MATCHING, NotificationRoleEnum.RALLY_INITIATOR, "RALLY_PARTICIPANT_QUIT", "参与者退出"),
    RALLY_PARTICIPANTS_FULL(NotificationModuleEnum.PLAY_MATCHING, NotificationRoleEnum.RALLY_INITIATOR, "RALLY_PARTICIPANTS_FULL", "人数已满"),

    // 参与者事件
    RALLY_APPLICATION_ACCEPTED(NotificationModuleEnum.PLAY_MATCHING, NotificationRoleEnum.RALLY_PARTICIPANT, "RALLY_APPLICATION_ACCEPTED", "申请被接受"),
    RALLY_APPLICATION_REJECTED(NotificationModuleEnum.PLAY_MATCHING, NotificationRoleEnum.RALLY_PARTICIPANT, "RALLY_APPLICATION_REJECTED", "申请被拒绝"),
    RALLY_CANCELLED(NotificationModuleEnum.PLAY_MATCHING, NotificationRoleEnum.RALLY_PARTICIPANT, "RALLY_CANCELLED", "约球已取消"),

    /**
     * SOCIAL 社交模块
     */
    SOCIAL_NOTE_LIKED(NotificationModuleEnum.SOCIAL, NotificationRoleEnum.SOCIAL_USER, "SOCIAL_NOTE_LIKED", "帖子被点赞"),
    SOCIAL_NOTE_COMMENTED(NotificationModuleEnum.SOCIAL, NotificationRoleEnum.SOCIAL_USER, "SOCIAL_NOTE_COMMENTED", "帖子被评论"),
    SOCIAL_FOLLOWED(NotificationModuleEnum.SOCIAL, NotificationRoleEnum.SOCIAL_USER, "SOCIAL_FOLLOWED", "被关注"),
    SOCIAL_NOTE_MENTIONED(NotificationModuleEnum.SOCIAL, NotificationRoleEnum.SOCIAL_USER, "SOCIAL_NOTE_MENTIONED", "被@提及"),

    /**
     * SYSTEM 系统模块
     */
    SYSTEM_ANNOUNCEMENT(NotificationModuleEnum.SYSTEM, NotificationRoleEnum.SYSTEM_USER, "SYSTEM_ANNOUNCEMENT", "系统公告"),
    SYSTEM_VERSION_UPDATE(NotificationModuleEnum.SYSTEM, NotificationRoleEnum.SYSTEM_USER, "SYSTEM_VERSION_UPDATE", "版本更新"),
    SYSTEM_MARKETING(NotificationModuleEnum.SYSTEM, NotificationRoleEnum.SYSTEM_USER, "SYSTEM_MARKETING", "营销推送");

    private final NotificationModuleEnum module;
    private final NotificationRoleEnum role;
    private final String eventCode;
    private final String description;

    /**
     * 生成完整的messageType编码
     * 格式：模块.角色.事件
     */
    public String getFullCode() {
        return module.getValue() + "." + role.getValue() + "." + eventCode;
    }

    /**
     * 验证事件是否属于指定的模块和角色
     */
    public boolean belongsTo(NotificationModuleEnum module, NotificationRoleEnum role) {
        return this.module == module && this.role == role;
    }

    /**
     * 通过完整编码获取事件
     */
    public static NotificationEventEnum fromFullCode(String fullCode) {
        for (NotificationEventEnum event : values()) {
            if (event.getFullCode().equals(fullCode)) {
                return event;
            }
        }
        throw new IllegalArgumentException("未知的事件编码: " + fullCode);
    }
}
