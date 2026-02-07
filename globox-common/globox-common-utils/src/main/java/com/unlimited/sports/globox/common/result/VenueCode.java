package com.unlimited.sports.globox.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 场馆模块错误码枚举
 * 错误码区间：7000-7999（场馆模块专用）
 */
@Getter
@AllArgsConstructor
public enum VenueCode implements ResultCode {

    // 预订相关 7001-7020
    BOOKING_SLOT_INFO_EMPTY(7001, "未选择预定时段,请选择您要预定的场次"),
    SLOT_BEING_BOOKED(7002, "该时段正在被其他用户预订中，请稍后重试或尝试其他时段"),
    SLOT_TEMPLATE_NOT_EXIST(7003, "所选时段已经失效,请刷新页面后重新选择"),
    SLOT_NOT_AVAILABLE(7004, "部分时段已不可用或已被占用，请重新选择可用时段"),
    SLOT_OCCUPIED(7005, "该时段已被其他用户预订，请重新选择"),
    SLOT_DIFFERENT_VENUE(7006, "暂不支持跨场馆预订，请确保所选场次均来自同一场馆"),
    VENUE_CAN_NOT_BOOKING(7007,"该场馆暂时不可预订,请联系商家或者尝试预定其他场馆"),
    VENUE_CAN_NOT_UNLOCK(7008,"无法解锁,请联系商家手动解锁"),
    VENUE_PRICE_NOT_CONFIGURED(7009, "该场地价格未配置,请联系商家或预定其他场地"),
    VENUE_TIME_SLOT_BOOKING_NOT_ALLOWED(7010, "不允许时段预定"),
    VENUE_MIN_BOOKING_DURATION(7011, "该场馆此时间段最少起订1小时"),

    // 活动相关 7021-7040
    ACTIVITY_PARAM_INVALID(7021, "活动信息加载失败，请尝试重新进入页面"),
    ACTIVITY_NOT_EXIST(7022, "该活动不存在或已下架，请尝试其他活动"),
    ACTIVITY_REGISTRATION_CLOSED(7023, "活动报名已截止，您可以关注后续相关活动"),
    ACTIVITY_ALREADY_REGISTERED(7024, "您已经报名过该活动，无法重复报名"),
    ACTIVITY_FULL(7025, "该活动名额已满，建议您关注其他场次或类似活动"),
    ACTIVITY_NO_SLOTS(7026, "该活动名额不足，建议您减少报名人数或选择其他活动"),
    ACTIVITY_ALREADY_STARTED(7027, "活动已开始，无法报名，请关注其他活动"),
    ACTIVITY_TYPE_NOT_SUPPORTED(7028, "不支持的活动类型，请联系客服"),
    ACTIVITY_QUOTA_EXCEEDED(7029, "活动报名数量超过限制，建议您减少报名人数或选择其他活动"),

    // 场馆场地相关 7041-7060
    VENUE_NOT_EXIST(7041, "未找到该场馆信息，请返回列表重新进入"),
    COURT_NOT_EXIST(7042, "场地信息已变更，请刷新页面后再试"),


    // 兜底错误
    VENUE_BOOKING_FAIL(7090,"订场失败，请检查网络后重试或联系客服");

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
