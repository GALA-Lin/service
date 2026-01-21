package com.unlimited.sports.globox.model.social.entity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum RallyPostsStatusEnum {
    PUBLISHED(0, "已发布"),
    FULL(1, "已满员"),
    CANCELLED(2, "已取消");

    private final int code;
    private final String description;

    RallyPostsStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RallyPostsStatusEnum fromCode(int code) {
        for (RallyPostsStatusEnum status : RallyPostsStatusEnum.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的状态码: " + code);
    }

    /**
     * 获取状态描述
     */
    public static String getDescriptionByCode(int code) {
        try {
            RallyPostsStatusEnum status = fromCode(code);
            return status.getDescription();
        } catch (IllegalArgumentException e) {
            log.error("获取球局状态描述失败，未知的状态码: {}", code);
            return "未知状态";
        }
    }

    @Override
    public String toString() {
        return this.description;
    }
}
