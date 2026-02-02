package com.unlimited.sports.globox.model.social.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@AllArgsConstructor
public enum RallyPostsStatusEnum {
    PUBLISHED(0, "已发布"),
    FULL(1, "已满员"),
    CANCELLED(2, "已取消"),
    COMPLETED(3, "已完成"); // 新增：已完成/已过期

    private final int code;
    private final String description;

    /**
     * 根据状态码获取状态枚举
     * @param code 状态码
     * @return 状态枚举
     */
    public static RallyPostsStatusEnum fromCode(int code) {
        for (RallyPostsStatusEnum status : RallyPostsStatusEnum.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的状态码: " + code);
    }

    /**
     * 根据状态码获取状态描述
     * @param code 状态码
     * @return 状态描述
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
