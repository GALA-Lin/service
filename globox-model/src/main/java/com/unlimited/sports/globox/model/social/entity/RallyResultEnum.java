package com.unlimited.sports.globox.model.social.entity;


import com.unlimited.sports.globox.common.result.ResultCode;

/**
 *  球帖结果枚举
 */
public enum RallyResultEnum implements ResultCode {
    RALLY_POSTS_PUBLISHED_SUCCESS(1,"发布成功"),
    RALLY_POSTS_PUBLISHED_FAILURE(2,"发布失败"),
    RALLY_POSTS_JOIN_SUCCESS(3,"申请加入成功"),
    RALLY_POSTS_JOIN_FAILURE(4,"申请加入失败"),
    RALLY_POSTS_JOIN_HAS_FULL (5,"该活动人员已满"),
    RALLY_POSTS_JOIN_HAS_CANCELLED(6,"该活动已取消"),
    RALLY_POSTS_CANCEL_SUCCESS(7,"取消成功"),
    RALLY_POSTS_CANCEL_FAILURE(8,"取消失败"),
    RALLY_POSTS_CANCEL_NOT_AUTHORIZED(9,"取消失败，无权限"),
    RALLY_POSTS_CANCEL_JOIN_SUCCESS(10,"取消成功"),
    RALLY_POSTS_JOIN_HAS_APPLIED(11,"该活动已申请"),
    RALLY_POSTS_INSPECT_NOT_AUTHORIZED(12,"检查失败，无权限"),
    RALLY_POSTS_INSPECT_SUCCESS_PASS(13,"检查成功，通过"),
    RALLY_POSTS_INSPECT_FAILURE(14,"检查失败"),
    RALLY_POSTS_INSPECT_HAS_CANCELLED(15,"检查失败，已取消"),
    RALLY_POSTS_UPDATE_NOT_AUTHORIZED(16,"更新失败，无权限"),
    RALLY_POSTS_UPDATE_SUCCESS(17,"更新成功"),
    RALLY_POSTS_JOIN_HAS_EXPIRED(18,"该活动已过期"),
    RALLY_POST_NOT_EXIST(19,"帖子不存在"),
    RALLY_POSTS_INSPECT_NOT_EXIST(20,"审核不存在"),
    RALLY_POSTS_CANCEL_JOIN_NOT_EXIST(21,"取消失败，你还未申请");
    private Integer code;
    private String message;
    RallyResultEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
    public Integer getCode() {
        return code;
    }
    public String getMessage() {
        return message;
    }
    public static String getMessageByCode(Integer code) {
        for (RallyResultEnum value : RallyResultEnum.values()) {
            if (value.getCode().equals(code)) {
                return value.getMessage();
            }
        }
        return null;
    }
}
