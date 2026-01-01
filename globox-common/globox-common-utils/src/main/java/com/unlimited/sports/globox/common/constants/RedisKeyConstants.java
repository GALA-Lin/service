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

    /**
     * 微信临时凭证前缀
     */
    public static final String WECHAT_TEMP_TOKEN_PREFIX = "wechat:temp:";
}

