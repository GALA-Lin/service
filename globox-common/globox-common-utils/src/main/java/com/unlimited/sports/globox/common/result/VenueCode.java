package com.unlimited.sports.globox.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 场馆模块错误码枚举
 * 错误码区间：1800-1899（场馆模块专用）
 */
@Getter
@AllArgsConstructor
public enum VenueCode implements ResultCode {

    // 预订相关 1801-1820
    BOOKING_SLOT_INFO_EMPTY(1801, "未选择预定时段,请选择您要预定的场次"),
    SLOT_BEING_BOOKED(1802, "该时段正在被其他用户预订中，请稍后重试或尝试其他时段"),
    SLOT_TEMPLATE_NOT_EXIST(1803, "所选时段已经失效,请刷新页面后重新选择"),
    SLOT_NOT_AVAILABLE(1804, "部分时段已不可用或已被占用，请重新选择可用时段"),
    SLOT_OCCUPIED(1805, "该时段已被其他用户预订，请重新选择"),
    SLOT_DIFFERENT_VENUE(1806, "暂不支持跨场馆预订，请确保所选场次均来自同一场馆"),

    // 活动相关 1821-1840
    ACTIVITY_PARAM_INVALID(1821, "活动信息加载失败，请尝试重新进入页面"),
    ACTIVITY_NOT_EXIST(1822, "该活动不存在或已下架，请尝试其他活动"),
    ACTIVITY_REGISTRATION_CLOSED(1823, "活动报名已截止，您可以关注后续相关活动"),
    ACTIVITY_ALREADY_REGISTERED(1824, "您已经报名过该活动，无法重复报名"),
    ACTIVITY_FULL(1825, "该活动名额已满，建议您关注其他场次或类似活动"),

    // 场馆场地相关 1841-1860
    VENUE_NOT_EXIST(1841, "未找到该场馆信息，请返回列表重新进入"),
    COURT_NOT_EXIST(1842, "场地信息已变更，请刷新页面后再试"),
    VENUE_PRICE_NOT_CONFIGURED(1843, "该场地暂未配置价格，请咨询商家或预订其他场地"),


    // 兜底错误
    VENUE_BOOKING_FAIL(1890,"订场失败，请检查网络后重试或联系客服");

    ;

    private final Integer code;
    private final String message;

    /**
     * 错误码到 VenueCode 的映射表
     */
    private static final Map<Integer, VenueCode> CODE_MAP = new HashMap<>();

    static {
        for (VenueCode venueCode : VenueCode.values()) {
            CODE_MAP.put(venueCode.getCode(), venueCode);
        }
    }

    /**
     * 根据错误码获取对应的 VenueCode
     * @param code 错误码
     * @return 对应的 VenueCode，如果没有匹配则返回 VENUE_NOT_EXIST
     */
    public static VenueCode fromCode(Integer code) {
        return CODE_MAP.getOrDefault(code, VENUE_NOT_EXIST);
    }
}
