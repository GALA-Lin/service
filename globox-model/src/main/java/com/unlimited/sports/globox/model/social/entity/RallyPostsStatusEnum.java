package com.unlimited.sports.globox.model.social.entity;

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

    @Override
    public String toString() {
        return this.description;
    }
}
