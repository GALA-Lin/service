package com.unlimited.sports.globox.model.coach.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2025/12/31 16:52
 * 审核状态枚举
 */
@Getter
@AllArgsConstructor
public enum CoachAuditStatusEnum {
    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝");

    private final Integer code;
    private final String description;


    public static CoachAuditStatusEnum fromValue(Integer code) {
        for (CoachAuditStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的审核状态: " + code);
    }
}
