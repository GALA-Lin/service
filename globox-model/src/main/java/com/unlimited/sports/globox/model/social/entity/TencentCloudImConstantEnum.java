package com.unlimited.sports.globox.model.social.entity;

public enum TencentCloudImConstantEnum {

    TIM_TEXT_ELEM("1", "TIMTextElem"),
    TIM_IMAGE_ELEM("2", "TIMImageElem"),
    TIM_SOUND_ELEM("3", "TIMSoundElem"),
    TIM_VIDEO_ELEM("4", "TIMVideoElem"),
    TIM_FILE_ELEM("5", "TIMFileElem"),
    TIM_LOCATION_ELEM("6", "TIMLocationElem");

    private final String code;
    private final String message;

    TencentCloudImConstantEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
