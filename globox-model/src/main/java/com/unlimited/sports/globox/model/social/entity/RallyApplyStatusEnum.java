package com.unlimited.sports.globox.model.social.entity;

public enum RallyApplyStatusEnum {
    PENDING(0, "待审核"),
    ACCEPTED(1, "已接受"),
    REJECTED(2, "已拒绝"),
    CANCELLED(3, "已取消");

    private final int code;
    private final String description;

    RallyApplyStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RallyApplyStatusEnum fromCode(int code) {
        for (RallyApplyStatusEnum status : RallyApplyStatusEnum.values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status code: " + code);
    }
    

}
