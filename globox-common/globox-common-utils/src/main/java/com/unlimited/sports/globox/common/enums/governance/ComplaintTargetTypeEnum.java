package com.unlimited.sports.globox.common.enums.governance;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ComplaintTargetTypeEnum {

    /**
     * IM 消息/会话内容
     */
    IM_MESSAGE(1, "IM会话/消息"),

    /**
     * 帖子
     */
    NOTE(2, "帖子"),

    /**
     * 帖子评论
     */
    NOTE_COMMENT(3, "帖子评论"),

    /**
     * 场馆评论
     */
    VENUE_COMMENT(4, "场馆评论"),

    /**
     * 用户信息（昵称/头像/签名/简介等）
     */
    USER_PROFILE(5, "用户信息");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    public static ComplaintTargetTypeEnum of(Integer code) {
        if (code == null) return null;
        for (ComplaintTargetTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }

    public static boolean isValid(Integer code) {
        return of(code) != null;
    }
}