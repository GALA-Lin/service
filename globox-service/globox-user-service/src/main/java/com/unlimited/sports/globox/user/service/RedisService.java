package com.unlimited.sports.globox.user.service;

/**
 * Redis服务接口
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
public interface RedisService {

    /**
     * 保存短信验证码
     *
     * @param phone         手机号
     * @param code          验证码
     * @param expireSeconds 过期时间（秒）
     */
    void saveSmsCode(String phone, String code, long expireSeconds);

    /**
     * 获取短信验证码
     *
     * @param phone 手机号
     * @return 验证码，不存在则返回null
     */
    String getSmsCode(String phone);

    /**
     * 删除短信验证码
     *
     * @param phone 手机号
     */
    void deleteSmsCode(String phone);

    /**
     * 检查是否可以发送短信（频率限制）
     *
     * @param phone 手机号
     * @return true=可以发送，false=频率过快
     */
    boolean canSendSms(String phone);

    /**
     * 获取短信发送频率限制的剩余秒数（如果未被限流返回-1）
     *
     * @param phone 手机号
     * @return 剩余秒数，未限流返回-1
     */
    long getSmsRateLimitSeconds(String phone);

    /**
     * 增加验证码错误次数
     *
     * @param phone 手机号
     * @return 当前错误次数
     */
    long incrementSmsError(String phone);

    /**
     * 获取验证码错误次数
     *
     * @param phone 手机号
     * @return 错误次数
     */
    long getSmsErrorCount(String phone);

    /**
     * 清除验证码错误计数
     *
     * @param phone 手机号
     */
    void clearSmsError(String phone);

    /**
     * 增加密码错误次数
     *
     * @param phone 手机号
     * @return 当前错误次数
     */
    long incrementPasswordError(String phone);

    /**
     * 获取密码错误次数
     *
     * @param phone 手机号
     * @return 错误次数
     */
    long getPasswordErrorCount(String phone);

    /**
     * 清除密码错误计数
     *
     * @param phone 手机号
     */
    void clearPasswordError(String phone);

    /**
     * 保存Refresh Token（双存储方案）
     *
     * @param userId        用户ID
     * @param refreshToken  Refresh Token字符串
     * @param expireSeconds 过期时间（秒）
     */
    void saveRefreshToken(Long userId, String refreshToken, long expireSeconds);
    void saveRefreshTokenWithClient(Long userId, String refreshToken, long expireSeconds, String clientType);

    /**
     * 验证Refresh Token是否有效
     *
     * @param refreshToken Refresh Token字符串
     * @return true=有效，false=无效
     */
    boolean isRefreshTokenValid(String refreshToken);

    /**
     * 删除Refresh Token
     *
     * @param refreshToken Refresh Token字符串
     * @param jwtSecret    JWT密钥（用于解析token）
     */
    void deleteRefreshToken(String refreshToken, String jwtSecret);

    /**
     * 清除用户的所有Refresh Token
     *
     * @param userId 用户ID
     */
    void deleteAllRefreshTokens(Long userId);
    void deleteRefreshTokensByClientType(Long userId, String clientType);

    /**
     * 保存 Access Token 的 jti（用于单点登录控制）
     *
     * @param userId 用户ID
     * @param clientType 客户端类型
     * @param jti token 唯一标识
     * @param expireSeconds 过期时间（秒）
     */
    void saveAccessTokenJti(Long userId, String clientType, String jti, long expireSeconds);

    /**
     * 获取 Access Token 的 jti
     *
     * @param userId 用户ID
     * @param clientType 客户端类型
     * @return jti，不存在返回 null
     */
    String getAccessTokenJti(Long userId, String clientType);

    /**
     * 删除 Access Token 的 jti
     *
     * @param userId 用户ID
     * @param clientType 客户端类型
     */
    void deleteAccessTokenJti(Long userId, String clientType);


    /**
     * 通用：设置键值（带过期时间）
     *
     * @param key           键
     * @param value         值
     * @param expireSeconds 过期时间（秒）
     */
    void set(String key, String value, long expireSeconds);

    /**
     * 通用：获取值
     *
     * @param key 键
     * @return 值，不存在则返回null
     */
    String get(String key);

    /**
     * 通用：删除键
     *
     * @param key 键
     */
    void delete(String key);

    /**
     * 保存微信临时凭证
     *
     * @param tempToken     临时凭证
     * @param identifier    微信标识（openid或unionid）
     * @param expireMinutes 过期时间（分钟）
     */
    void saveWechatTempToken(String tempToken, String identifier, long expireMinutes);

    /**
     * 获取微信临时凭证
     *
     * @param tempToken 临时凭证
     * @return 微信标识（openid或unionid），不存在则返回null
     */
    String getWechatTempToken(String tempToken);

    /**
     * 删除微信临时凭证
     *
     * @param tempToken 临时凭证
     */
    void deleteWechatTempToken(String tempToken);

    /**
     * 原子自增并设置过期时间（首次自增时）
     *
     * @param key           键
     * @param expireSeconds 过期时间（秒）
     * @return 自增后的值
     */
    long increment(String key, long expireSeconds);

    void clearSmsRateLimit(String phone);
}
