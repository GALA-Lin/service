package com.unlimited.sports.globox.model.social.entity;

public enum OperationTypeEnum {
    UPDATE_LAST_MESSAGE("UPDATE_LAST_MESSAGE"),
    INCREMENT_UNREAD("INCREMENT_UNREAD"),
    CLEAR_UNREAD("CLEAR_UNREAD");
    private String message;
    OperationTypeEnum(String message) {
        this.message = message;
    }
    public String getValue() {
        return message;
    }
}
