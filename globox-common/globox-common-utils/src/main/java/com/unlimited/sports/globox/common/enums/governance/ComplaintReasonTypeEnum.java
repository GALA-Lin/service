package com.unlimited.sports.globox.common.enums.governance;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ComplaintReasonTypeEnum {

    /**
     * 传播谣言 / 虚假信息
     */
    RUMOR(1, "传播谣言/虚假信息"),

    /**
     * 煽动对立 / 制造恐慌
     */
    PANIC(2, "煽动对立/制造恐慌"),

    /**
     * 诈骗 / 引流（诱导转账、私聊、跳转外部平台等）
     */
    FRAUD(3, "诈骗/引流"),

    /**
     * 色情低俗 / 性暗示
     */
    PORNOGRAPHY(4, "色情低俗"),

    /**
     * 暴恐血腥 / 危险行为
     */
    VIOLENCE(5, "暴恐血腥/危险行为"),

    /**
     * 人身攻击 / 骚扰 / 辱骂
     */
    HARASSMENT(6, "人身攻击/骚扰辱骂"),

    /**
     * 垃圾广告 / 营销刷屏
     */
    SPAM_AD(7, "垃圾广告/营销刷屏"),

    /**
     * 侵犯隐私 / 泄露个人信息
     */
    PRIVACY(8, "侵犯隐私/泄露个人信息"),

    /**
     * 其他（由用户补充说明）
     */
    OTHER(9, "其他");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    public static ComplaintReasonTypeEnum of(Integer code) {
        if (code == null) return null;
        for (ComplaintReasonTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }

    public static boolean isValid(Integer code) {
        return of(code) != null;
    }
}