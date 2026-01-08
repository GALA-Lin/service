package com.unlimited.sports.globox.model.social.entity;

public enum IsInitiatorForRallyEnum {
    NO(0, "非发起人"),
    YES(1, "发起人");
    private final int code;
    private final String message;
    IsInitiatorForRallyEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
    public int getCode() {
        return code;
    }
    public String getMessage() {
        return message;
    }
    public static IsInitiatorForRallyEnum getByCode(int code) {
        for (IsInitiatorForRallyEnum genderLimit : values()) {
            if (genderLimit.getCode() == code) {
                return genderLimit;
            }
        }
        throw new IllegalArgumentException("Invalid gender limit code: " + code);
    }
}
