package com.unlimited.sports.globox.model.social.entity;


import com.unlimited.sports.globox.common.result.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *  球帖结果枚举
 *  错误码区间：3036-3099（社交模块专用）
 */
@Getter
@AllArgsConstructor
public enum RallyResultEnum implements ResultCode {
    RALLY_POSTS_PUBLISHED_SUCCESS(3036,"发布成功"),
    RALLY_POSTS_PUBLISHED_FAILURE(3037,"发布失败"),
    RALLY_POSTS_JOIN_SUCCESS(3038,"申请加入成功"),
    RALLY_POSTS_JOIN_FAILURE(3039,"申请加入失败"),
    RALLY_POSTS_JOIN_HAS_FULL (3040,"该活动人员已满"),
    RALLY_POSTS_JOIN_HAS_CANCELLED(3041,"该活动已取消"),
    RALLY_POSTS_CANCEL_SUCCESS(3042,"取消成功"),
    RALLY_POSTS_CANCEL_FAILURE(3043,"取消失败"),
    RALLY_POSTS_CANCEL_NOT_AUTHORIZED(3044,"取消失败，无权限"),
    RALLY_POSTS_CANCEL_JOIN_SUCCESS(3045,"取消成功"),
    RALLY_POSTS_JOIN_HAS_APPLIED(3046,"该活动已申请"),
    RALLY_POSTS_INSPECT_NOT_AUTHORIZED(3047,"检查失败，无权限"),
    RALLY_POSTS_INSPECT_SUCCESS_PASS(3048,"检查成功，通过"),
    RALLY_POSTS_INSPECT_FAILURE(3049,"检查失败"),
    RALLY_POSTS_INSPECT_HAS_CANCELLED(3050,"检查失败，已取消"),
    RALLY_POSTS_UPDATE_NOT_AUTHORIZED(3051,"更新失败，无权限"),
    RALLY_POSTS_UPDATE_SUCCESS(3052,"更新成功"),
    RALLY_POSTS_JOIN_HAS_EXPIRED(3053,"该活动已过期"),
    RALLY_POST_NOT_EXIST(3054,"帖子不存在"),
    RALLY_POSTS_INSPECT_NOT_EXIST(3055,"审核不存在"),
    RALLY_POSTS_CANCEL_JOIN_NOT_EXIST(3056,"取消失败，你还未申请"),
    RALLY_POSTS_JOIN_NTRP_LIMIT(3057,"申请加入失败，不符合NTRP限制"),
    RALLY_POSTS_APPLY_SELF_FORBIDDEN(3058,"不能申请自己的约球" ),
    RALLY_POSTS_CANCEL_JOIN_ALREADY_CANCELLED(3059, "你已经取消了该申请");

    private final Integer code;
    private final String message;

    public static String getMessageByCode(Integer code) {
        for (RallyResultEnum value : RallyResultEnum.values()) {
            if (value.getCode().equals(code)) {
                return value.getMessage();
            }
        }
        return null;
    }
}
