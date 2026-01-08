package com.unlimited.sports.globox.model.social.entity;

/**
 * 约球支付方式枚举
 */
public enum RallyCostBearerEnum {
    INITIATOR_BEAR(0, "发起人承担"),
    AA_SHARE(1, "AA分摊");
    private int code;
    private String message;
    RallyCostBearerEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
    public int getCode() {
        return code;
    }
    public String getDesc() {
        return message;
    }
    public static String getDescByCode(int code) {
        for (RallyCostBearerEnum value : RallyCostBearerEnum.values()) {
            if (value.getCode() == code) {
                return value.getDesc();
            }
        }
        return null;
    }
}
