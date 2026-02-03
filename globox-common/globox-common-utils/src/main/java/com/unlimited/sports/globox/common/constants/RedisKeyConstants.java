package com.unlimited.sports.globox.common.constants;

/**
 * Redis Key 常量
 * 统一管理所有 Redis key 前缀，便于复用和维护
 *
 * @author Wreckloud
 * @since 2025/12/22
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    /**
     * 短信验证码前缀
     */
    public static final String SMS_CODE_PREFIX = "sms:code:";

    /**
     * 短信发送频率限制前缀
     */
    public static final String SMS_RATE_PREFIX = "sms:rate:";

    /**
     * 短信验证码错误次数前缀
     */
    public static final String SMS_ERROR_PREFIX = "sms:error:";

    /**
     * 密码错误次数前缀
     */
    public static final String PASSWORD_ERROR_PREFIX = "password:error:";

    /**
     * Refresh Token 前缀（用于校验）
     */
    public static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * Refresh Token 用户索引前缀（用于批量删除）
     */
    public static final String REFRESH_TOKEN_USER_PREFIX = "refresh_token:user:";
    public static final String REFRESH_TOKEN_USER_CLIENT_PREFIX = "refresh_token:user_client:";

    /**
     * Access Token JTI 缓存前缀（用于单点登录控制）
     */
    public static final String ACCESS_TOKEN_JTI_PREFIX = "access_token:jti:";


    /**
     * 微信临时凭证前缀
     */
    public static final String WECHAT_TEMP_TOKEN_PREFIX = "wechat:temp:";

    /**
     * 球盒号序列前缀（按日期）
     */
    public static final String GLOBOX_NO_SEQ_PREFIX = "globox:no:seq:";
}

