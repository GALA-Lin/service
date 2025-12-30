package com.unlimited.sports.globox.model.venue.enums;

public enum VenueStatus {
    SUSPENDED(0, "暂停营业"),
    NORMAL(1, "正常营业");

    private final int value;
    private final String description;

    VenueStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static VenueStatus fromValue(int value) {
        for (VenueStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown VenueStatus value: " + value);
    }
}
