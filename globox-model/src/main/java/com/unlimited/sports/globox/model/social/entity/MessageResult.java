package com.unlimited.sports.globox.model.social.entity;


import lombok.Getter;

@Getter
public enum MessageResult {
    SUCCESS("消息发送成功"),
    FAILURE("消息发送失败"),
    CANCEL("消息取消发送"),
    MESSAGE_IS_NULL("消息不存在"),
    MESSAGE_HAS_EXISTED("消息已存在"),
    NOT_PERMISSION_TO_RECALL_OTHERS_MESSAGES("无权撤回他人消息"),
    OVER_RECALL_TIME_LIMIT("已超过撤回时间限制"),
    MESSAGE_RECALL_SUCCESS("消息撤回成功"),
    NOT_PERMISSION_TO_SET_MESSAGE_AS_READ("无权设置此消息已读"),
    MESSAGE_AS_READ_OK("设置已读成功"),
    NOT_PERMISSION_TO_DELETE_OTHERS_MESSAGES("无权删除他人消息"),
    MESSAGE_DELETE_SUCCESS("消息删除成功"),
    MESSAGE_DELETE_FAILURE("消息删除失败"),
    NOT_PERMISSION_TO_SELF("不能给自己发送消息"),
    MESSAGE_IMPORT_SUCCESS("导入成功");
    private final String message;
    MessageResult(String message) {
        this.message = message;
    }
}
