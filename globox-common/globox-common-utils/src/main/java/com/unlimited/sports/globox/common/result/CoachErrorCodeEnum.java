package com.unlimited.sports.globox.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 教练预约业务错误码
 * 4000-4999
 */
@Getter
@AllArgsConstructor
public enum CoachErrorCodeEnum implements ResultCode {

    PARAM_SLOT_EMPTY(4000, "预约时段信息不能为空"),
    PARAM_COACH_USER_ID_EMPTY(4001, "教练用户ID不能为空"),
    PARAM_USER_ID_EMPTY(4002, "用户ID不能为空"),
    PARAM_SLOT_ID_EMPTY(4003, "时段记录ID不能为空"),
    PARAM_BOOKING_DATE_EMPTY(4004, "预约日期不能为空"),
    COACH_CANNOT_BOOK_SELF(4005,"您不能预约自己的课程"),
    MIN_HOURS_NOT_MET(4006, "未满足最低课时要求"),
    REMOTE_AREA_MIN_HOURS_NOT_MET(4007, "远距离区域未满足最低课时要求"),

    COACH_INFO_NOT_EXIST(4010, "教练信息不存在"),
    COACH_BASE_INFO_GET_FAILED(4011, "无法获取教练基本信息"),
    COACH_PROFILE_NOT_COMPLETE(4012, "教练资料未完善，暂不支持预约"),

    SLOT_NOT_EXIST(4020, "部分时段不存在"),
    SLOT_COACH_NOT_MATCH(4021, "所选时段必须来自同一教练"),
    SLOT_DATE_NOT_SAME(4022, "所选时段必须在同一天"),
    SLOT_NOT_CONTINUOUS(4023, "所选时段必须是连续的"),
    SLOT_UNAVAILABLE(4024, "时段状态不可用或已被其他用户锁定"),
    SLOT_LOCK_FAILED(4025, "时段锁定失败，可能已被其他用户占用"),
    SLOT_STATUS_ABNORMAL(4026, "时段状态异常，请刷新后重试"),

    LOCK_ACQUIRE_FAILED(4030, "时段正在被其他用户预约，请稍后重试"),
    LOCK_RELEASE_FAILED(4031, "分布式锁释放失败（非业务异常）"),
    LOCK_KEY_BUILD_FAILED(4032, "分布式锁Key构建失败"),

    TRANSACTION_EXECUTE_FAILED(4040, "预约计价事务执行失败"),
    DATA_CONSISTENCY_ERROR(4041, "数据一致性校验失败，请刷新后重试"),

    USER_INFO_GET_FAILED(4050, "无法获取用户基本信息"),
    USER_AUTH_FAILED(4051, "用户权限验证失败，无预约权限"),

    PRICING_CALC_FAILED(4060, "教练预约价格计算失败"),
    TEMPLATE_PRICE_NOT_EXIST(4061, "时段模板价格未配置"),
    SERVICE_TYPE_UNKNOWN(4062, "未知的教练服务类型"),
    SERVICE_TYPE_NOT_SUPPORT(4063, "该时段教练服务类型不支持预约"),

    SLOT_UNLOCK_FAILED(4070, "时段解锁失败"),
    SLOT_UNLOCK_NO_PERMISSION(4071, "无权限解锁该时段（非当前用户锁定）"),
    SLOT_UNLOCK_EMPTY(4072, "无需要解锁的时段");

    private final Integer code;

    private final String message;
}
