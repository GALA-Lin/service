package com.unlimited.sports.globox.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录认证相关响应码枚举
 * 错误码区间：2000-2999（认证授权模块专用）
 */
@Getter
@AllArgsConstructor
public enum UserAuthCode implements ResultCode {

    // 登录注册相关 2001-2009
    INVALID_PHONE(2001, "手机号格式不正确"),
    NOT_IN_WHITELIST(2002, "当前仅限内测用户，敬请期待正式上线"),
    SMS_SEND_TOO_FREQUENT(2003, "验证码发送过于频繁，请稍后再试"),
    INVALID_CAPTCHA(2004, "验证码错误或已过期"),
    CAPTCHA_ERROR_TOO_MANY(2005, "验证码错误次数过多，请重新获取"),
    
    // 密码相关 2010-2019
    USER_NOT_EXIST(2010, "用户不存在"),
    PASSWORD_WRONG(2011, "密码错误"),
    PASSWORD_NOT_SET(2012, "未设置密码，请使用验证码登录"),
    PASSWORD_TOO_WEAK(2013, "密码必须为6-20位"),
    PASSWORD_MISMATCH(2014, "两次输入的密码不一致"),
    LOGIN_FAILED(2015, "手机号或密码错误"),
    PASSWORD_ALREADY_SET(2016, "已设置过密码，请使用修改密码"),
    
    // Token相关 2020-2029
    TOKEN_EXPIRED(2020, "登录已过期，请重新登录"),
    TOKEN_INVALID(2021, "登录已失效，请重新登录"),
    REFRESH_TOKEN_INVALID(2022, "Refresh Token无效或已过期"),
    
    // RPC查询相关 2030-2039
    BATCH_QUERY_TOO_LARGE(2030, "批量查询数量超过限制，最多50个"),
    QUERY_NOT_EXIST(2031, "查询的用户信息不存在"),
    USER_PHONE_NOT_BOUND(2032, "手机号未绑定"),

    // 微信登录相关 2033-2039
    WECHAT_AUTH_FAILED(2033, "微信授权失败"),
    WECHAT_CODE_EXPIRED(2034, "微信授权code已过期"),
    TEMP_TOKEN_EXPIRED(2035, "临时凭证已过期，请重新授权"),
    IDENTITY_ALREADY_BOUND(2036, "该身份已绑定其他账号"),
    WECHAT_CONFIG_INVALID(2037, "微信配置缺失或不匹配"),
    CLIENT_TYPE_UNSUPPORTED(2038, "不支持的客户端类型"),
    
    // 用户资料相关 2040-2059
    INVALID_PARAM(2040, "参数无效"),
    INVALID_RACKET_ID(2041, "球拍型号不存在"),
    MULTIPLE_PRIMARY_RACKET(2042, "只能设置一个主力拍"),
    INVALID_STYLE_TAG(2043, "球风标签无效或不存在"),
    MEDIA_COUNT_EXCEEDED(2044, "媒体数量超过限制，最多12条（图+视频合计）"),
    VIDEO_COUNT_EXCEEDED(2045, "视频数量超过限制，最多5条"),
    MEDIA_SORT_DUPLICATE(2046, "排序值不能重复"),
    INVALID_RACKET_LEVEL(2047, "球拍层级无效，必须为MODEL"),
    INACTIVE_RACKET_MODEL(2048, "球拍型号已下架或不可用"),
    INVALID_REGION(2057, "地区无效或不存在"),
    USERNAME_INVALID_FORMAT(2058, "球盒号格式不正确，仅支持9位数字"),
    USERNAME_ALREADY_TAKEN(2059, "球盒号已被占用，请换一个试试"),
    USERNAME_SET_FAILED(2060, "球盒号设置失败，请稍后再试"),

    MISSING_USER_ID_HEADER(2049, "缺少用户ID请求头"),
    MISSING_UPLOAD_FILE(2050, "缺少上传文件"),
    UPLOAD_FILE_TOO_LARGE(2051, "上传文件过大"),
    UPLOAD_FILE_TYPE_NOT_SUPPORTED(2052, "文件类型不支持"),
    UPLOAD_FILE_FAILED(2053, "文件上传失败"),
    MEDIA_URL_REQUIRED(2054, "媒体地址不能为空"),
    VIDEO_COVER_REQUIRED(2055, "视频封面不能为空"),
    SYNC_USER_PROFILE_ERROR(2056,"同步用户数据失败"),

    // 球星卡肖像抠图相关 2056-2059
    PORTRAIT_MATTING_FAILED(2056, "球星卡肖像处理失败，请稍后重试"),

    // 商家登录相关 2060-2069
    MERCHANT_ACCOUNT_NOT_EXIST(2060, "账号不存在，请检查账号是否正确或联系管理员"),
    MERCHANT_PASSWORD_ERROR(2061, "账号或密码错误，请重新输入"),
    MERCHANT_ACCOUNT_DISABLED(2062, "账号已被禁用，请联系管理员"),

    // 用户账号状态相关 2063-2069
    USER_ACCOUNT_DISABLED(2063, "账号已被禁用，请联系管理员"),
    USER_ACCOUNT_CANCELLED(2064, "账号已注销，请重新注册"),
    CANCEL_CONFIRM_EXPIRED(2065, "注销确认已过期，请重新验证"),
    USER_REALNAME_REQUIRED(2066, "未实名"),
    USERNAME_COOLDOWN_NOT_EXPIRED(2067, "球盒号修改冷却期未到，请稍后再试"),
    ;

    private final Integer code;
    private final String message;
}
