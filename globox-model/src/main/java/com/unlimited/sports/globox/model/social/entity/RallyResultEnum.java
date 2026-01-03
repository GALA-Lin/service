package com.unlimited.sports.globox.model.social.entity;


public enum RallyResultEnum {
    RALLY_POSTS_PUBLISHED_SUCCESS(1,"发布成功"),
    RALLY_POSTS_PUBLISHED_FAILURE(2,"发布失败"),
    RALLY_POSTS_JOIN_SUCCESS(3,"加入成功"),
    RALLY_POSTS_JOIN_FAILURE(4,"加入失败"),
    RALLY_POSTS_JOIN_HAS_FULL (5,"该活动人员已满"),
    RALLY_POSTS_JOIN_HAS_CANCELLED(6,"该活动已取消"),
    RALLY_POSTS_CANCEL_SUCCESS(7,"取消成功"),
    RALLY_POSTS_CANCEL_FAILURE(8,"取消失败"),
    RALLY_POSTS_CANCEL_NOT_AUTHORIZED(9,"取消失败，无权限"),
    RALLY_POSTS_CANCEL_JOIN_SUCCESS(10,"取消成功，已加入");
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
