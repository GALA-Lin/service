package com.unlimited.sports.globox.common.enums.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

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
    VENUE_REFUND_APPROVED(NotificationModuleEnum.VENUE_BOOKING, NotificationRoleEnum.VENUE_BOOKER, "VENUE_REFUND_APPROVED", "退款申请已通过"),
    VENUE_BOOKING_REMINDER(NotificationModuleEnum.VENUE_BOOKING, NotificationRoleEnum.VENUE_BOOKER, "VENUE_BOOKING_REMINDER", "订场即将开始提醒"),
    ACTIVITY_BOOKING_REMINDER(NotificationModuleEnum.VENUE_BOOKING, NotificationRoleEnum.VENUE_BOOKER, "ACTIVITY_BOOKING_REMINDER", "活动即将开始提醒"),

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
    COACH_CLASS_BOOKER_REMINDER(NotificationModuleEnum.COACH_BOOKING, NotificationRoleEnum.COACH_BOOKER, "COACH_CLASS_BOOKER_REMINDER", "您的教练课程即将开始"),

    // 教练事件
    COACH_PROVIDER_APPOINTMENT_CREATED(NotificationModuleEnum.COACH_BOOKING, NotificationRoleEnum.COACH_PROVIDER, "COACH_PROVIDER_APPOINTMENT_CREATED", "预约已创建"),
    COACH_PROVIDER_APPOINTMENT_CANCELLED(NotificationModuleEnum.COACH_BOOKING, NotificationRoleEnum.COACH_PROVIDER, "COACH_PROVIDER_APPOINTMENT_CANCELLED", "预约已取消"),
    COACH_PROVIDER_PAYMENT_RECEIVED(NotificationModuleEnum.COACH_BOOKING, NotificationRoleEnum.COACH_PROVIDER, "COACH_PROVIDER_PAYMENT_RECEIVED", "收到课程费用"),
    COACH_PROVIDER_REFUND_APPLIED(NotificationModuleEnum.COACH_BOOKING, NotificationRoleEnum.COACH_PROVIDER, "COACH_PROVIDER_REFUND_APPLIED", "收到预约退款申请"),
    COACH_PROVIDER_APPROVAL_PASSED(NotificationModuleEnum.COACH_BOOKING, NotificationRoleEnum.COACH_PROVIDER, "COACH_PROVIDER_APPROVAL_PASSED", "审核已通过"),
    COACH_PROVIDER_APPROVAL_FAILED(NotificationModuleEnum.COACH_BOOKING, NotificationRoleEnum.COACH_PROVIDER, "COACH_PROVIDER_APPROVAL_FAILED", "审核未通过"),
    COACH_CLASS_PROVIDER_REMINDER(NotificationModuleEnum.COACH_BOOKING, NotificationRoleEnum.COACH_PROVIDER, "COACH_CLASS_PROVIDER_REMINDER","您有一节课程即将开始"
    ),
    /**
     * PLAY_MATCHING 约球模块
     */

    // 发起人事件
    RALLY_PARTICIPANT_APPLICATION(NotificationModuleEnum.PLAY_MATCHING, NotificationRoleEnum.RALLY_INITIATOR, "RALLY_PARTICIPANT_APPLICATION", "有人申请加入"),
    RALLY_PARTICIPANT_QUIT(NotificationModuleEnum.PLAY_MATCHING, NotificationRoleEnum.RALLY_INITIATOR, "RALLY_PARTICIPANT_QUIT", "参与者退出"),
    RALLY_PARTICIPANTS_FULL(NotificationModuleEnum.PLAY_MATCHING, NotificationRoleEnum.RALLY_INITIATOR, "RALLY_PARTICIPANTS_FULL", "人数已满"),

    // 参与者事件
    RALLY_APPLICATION_ACCEPTED(NotificationModuleEnum.PLAY_MATCHING, NotificationRoleEnum.RALLY_PARTICIPANT, "RALLY_APPLICATION_ACCEPTED", "申请被接受"),
    RALLY_CANCELLED(NotificationModuleEnum.PLAY_MATCHING, NotificationRoleEnum.RALLY_PARTICIPANT, "RALLY_CANCELLED", "约球已取消"),
    RALLY_PARTICIPANTS_FULL_ACCEPTED(NotificationModuleEnum.PLAY_MATCHING, NotificationRoleEnum.RALLY_PARTICIPANT, "RALLY_PARTICIPANTS_FULL_ACCEPTED", "人数已满，您已成功加入"),
    RALLY_PARTICIPANTS_FULL_REJECTED(NotificationModuleEnum.PLAY_MATCHING, NotificationRoleEnum.RALLY_PARTICIPANT, "RALLY_PARTICIPANTS_FULL_REJECTED", "人数已满，您的申请未被通过"
    ),
    /**
     * SOCIAL 社交模块
     */
    SOCIAL_NOTE_LIKED(NotificationModuleEnum.SOCIAL, NotificationRoleEnum.SOCIAL_USER, "SOCIAL_NOTE_LIKED", "帖子被点赞"),
    SOCIAL_NOTE_COMMENTED(NotificationModuleEnum.SOCIAL, NotificationRoleEnum.SOCIAL_USER, "SOCIAL_NOTE_COMMENTED", "帖子被评论"),
    SOCIAL_FOLLOWED(NotificationModuleEnum.SOCIAL, NotificationRoleEnum.SOCIAL_USER, "SOCIAL_FOLLOWED", "被关注"),
    SOCIAL_NOTE_MENTIONED(NotificationModuleEnum.SOCIAL, NotificationRoleEnum.SOCIAL_USER, "SOCIAL_NOTE_MENTIONED", "被@提及"),
    SOCIAL_CHAT_MESSAGE_RECEIVED(NotificationModuleEnum.SOCIAL, NotificationRoleEnum.SOCIAL_USER, "SOCIAL_CHAT_MESSAGE_RECEIVED", "收到一条新消息"
    ),

    /**
     * SYSTEM 系统模块
     */
    SYSTEM_ANNOUNCEMENT(NotificationModuleEnum.SYSTEM, NotificationRoleEnum.SYSTEM_USER, "SYSTEM_ANNOUNCEMENT", "系统公告"),
    SYSTEM_VERSION_UPDATE(NotificationModuleEnum.SYSTEM, NotificationRoleEnum.SYSTEM_USER, "SYSTEM_VERSION_UPDATE", "版本更新"),
    SYSTEM_MARKETING(NotificationModuleEnum.SYSTEM, NotificationRoleEnum.SYSTEM_USER, "SYSTEM_MARKETING", "营销推送"),
    SYSTEM_ACCOUNT_LOGIN_ELSEWHERE(NotificationModuleEnum.SYSTEM, NotificationRoleEnum.SYSTEM_USER, "SYSTEM_ACCOUNT_LOGIN_ELSEWHERE", "账号在别处登录");

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
        return Arrays.stream(values())
                .filter(event -> Objects.equals(event.getFullCode(),fullCode))
                .findFirst()
                .orElse(null);
    }
}
