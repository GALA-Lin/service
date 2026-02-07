package com.unlimited.sports.globox.user.service.impl;

import com.unlimited.sports.globox.user.service.RedisService;
import com.unlimited.sports.globox.common.utils.JwtUtil;
import com.unlimited.sports.globox.common.constants.RedisKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis服务实现
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
@Service
@Slf4j
public class RedisServiceImpl implements RedisService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void saveSmsCode(String phone, String code, long expireSeconds) {
        String key = RedisKeyConstants.SMS_CODE_PREFIX + phone;
        stringRedisTemplate.opsForValue().set(key, code, expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public String getSmsCode(String phone) {
        String key = RedisKeyConstants.SMS_CODE_PREFIX + phone;
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void deleteSmsCode(String phone) {
        String key = RedisKeyConstants.SMS_CODE_PREFIX + phone;
        stringRedisTemplate.delete(key);
    }

    @Override
    public boolean canSendSms(String phone) {
        String key = RedisKeyConstants.SMS_RATE_PREFIX + phone;
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 60, TimeUnit.SECONDS));
    }

    @Override
    public long getSmsRateLimitSeconds(String phone) {
        String key = RedisKeyConstants.SMS_RATE_PREFIX + phone;
        Long expire = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire == null ? -1 : expire;
    }

    @Override
    public long incrementSmsError(String phone) {
        String key = RedisKeyConstants.SMS_ERROR_PREFIX + phone;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, 5, TimeUnit.MINUTES);
        }
        return count == null ? 0 : count;
    }

    @Override
    public long getSmsErrorCount(String phone) {
        String key = RedisKeyConstants.SMS_ERROR_PREFIX + phone;
        String count = stringRedisTemplate.opsForValue().get(key);
        return count == null ? 0 : Long.parseLong(count);
    }

    @Override
    public void clearSmsError(String phone) {
        String key = RedisKeyConstants.SMS_ERROR_PREFIX + phone;
        stringRedisTemplate.delete(key);
    }

    @Override
    public long incrementPasswordError(String phone) {
        String key = RedisKeyConstants.PASSWORD_ERROR_PREFIX + phone;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, 15, TimeUnit.MINUTES);
        }
        return count == null ? 0 : count;
    }

    @Override
    public long getPasswordErrorCount(String phone) {
        String key = RedisKeyConstants.PASSWORD_ERROR_PREFIX + phone;
        String count = stringRedisTemplate.opsForValue().get(key);
        return count == null ? 0 : Long.parseLong(count);
    }

    @Override
    public void clearPasswordError(String phone) {
        String key = RedisKeyConstants.PASSWORD_ERROR_PREFIX + phone;
        stringRedisTemplate.delete(key);
    }

    @Override
    public void saveRefreshToken(Long userId, String refreshToken, long expireSeconds) {
        String hash = sha256(refreshToken);
        String userIdStr = String.valueOf(userId);
        // 双存储：校验用key存储userId，批量删除用key存储1
        String checkKey = RedisKeyConstants.REFRESH_TOKEN_PREFIX + hash;
        String userIndexKey = RedisKeyConstants.REFRESH_TOKEN_USER_PREFIX + userIdStr + ":" + hash;
        stringRedisTemplate.opsForValue().set(checkKey, userIdStr, expireSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(userIndexKey, "1", expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void saveRefreshTokenWithClient(Long userId, String refreshToken, long expireSeconds, String clientType) {
        saveRefreshToken(userId, refreshToken, expireSeconds);
        if (!StringUtils.hasText(clientType)) {
            return;
        }
        String hash = sha256(refreshToken);
        String key = RedisKeyConstants.REFRESH_TOKEN_USER_CLIENT_PREFIX
                + userId + ":" + clientType + ":" + hash;
        stringRedisTemplate.opsForValue().set(key, "1", expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean isRefreshTokenValid(String refreshToken) {
        String hash = sha256(refreshToken);
        String key = RedisKeyConstants.REFRESH_TOKEN_PREFIX + hash;
        Boolean hasKey = stringRedisTemplate.hasKey(key);
        return hasKey != null && hasKey;
    }

    @Override
    public void deleteRefreshToken(String refreshToken, String jwtSecret) {
        try {
            // 从token中解析userId（使用传入的secret）
            String userId = JwtUtil.getSubject(refreshToken, jwtSecret);
            String hash = sha256(refreshToken);
            // 删除两个key
            String checkKey = RedisKeyConstants.REFRESH_TOKEN_PREFIX + hash;
            String userIndexKey = RedisKeyConstants.REFRESH_TOKEN_USER_PREFIX + userId + ":" + hash;
            stringRedisTemplate.delete(checkKey);
            stringRedisTemplate.delete(userIndexKey);
        } catch (Exception e) {
            log.warn("删除Refresh Token失败，token可能已过期或无效：{}", e.getMessage());
        }
    }

    @Override
    public void saveAccessTokenJti(Long userId, String clientType, String jti, long expireSeconds) {
        String key = RedisKeyConstants.ACCESS_TOKEN_JTI_PREFIX + clientType + ":" + userId;
        stringRedisTemplate.opsForValue().set(key, jti, expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public String getAccessTokenJti(Long userId, String clientType) {
        String key = RedisKeyConstants.ACCESS_TOKEN_JTI_PREFIX + clientType + ":" + userId;
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void deleteAccessTokenJti(Long userId, String clientType) {
        String key = RedisKeyConstants.ACCESS_TOKEN_JTI_PREFIX + clientType + ":" + userId;
        stringRedisTemplate.delete(key);
    }

    @Override
    public void set(String key, String value, long expireSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    public void saveWechatTempToken(String tempToken, String identifier, long expireMinutes) {
        String key = RedisKeyConstants.WECHAT_TEMP_TOKEN_PREFIX + tempToken;
        stringRedisTemplate.opsForValue().set(key, identifier, expireMinutes, TimeUnit.MINUTES);
    }

    @Override
    public String getWechatTempToken(String tempToken) {
        String key = RedisKeyConstants.WECHAT_TEMP_TOKEN_PREFIX + tempToken;
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void deleteWechatTempToken(String tempToken) {
        String key = RedisKeyConstants.WECHAT_TEMP_TOKEN_PREFIX + tempToken;
        stringRedisTemplate.delete(key);
    }

    @Override
    public long increment(String key, long expireSeconds) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
        }
        return count == null ? 0 : count;
    }

    @Override
    public void clearSmsRateLimit(String phone) {
        stringRedisTemplate.delete(RedisKeyConstants.SMS_RATE_PREFIX + phone);
    }

    @Override
    public void deleteAllRefreshTokens(Long userId) {
        // 通过userId索引查找所有refresh token
        String pattern = RedisKeyConstants.REFRESH_TOKEN_USER_PREFIX + String.valueOf(userId) + ":*";
        Set<String> userIndexKeys = stringRedisTemplate.keys(pattern);
        if (userIndexKeys != null && !userIndexKeys.isEmpty()) {
            // 从索引key中提取hash，删除对应的校验key
            for (String userIndexKey : userIndexKeys) {
                // 提取hash：refresh_token:user:{userId}:{hash}
                String hash = userIndexKey.substring(userIndexKey.lastIndexOf(":") + 1);
                String checkKey = RedisKeyConstants.REFRESH_TOKEN_PREFIX + hash;
                stringRedisTemplate.delete(checkKey);
            }
            // 删除所有索引key
            stringRedisTemplate.delete(userIndexKeys);
            log.info("清除用户所有Refresh Token：userId={}, count={}", userId, userIndexKeys.size());
        }
    }


    @Override
    public void deleteRefreshTokensByClientType(Long userId, String clientType) {
        if (!StringUtils.hasText(clientType)) {
            return;
        }
        String pattern = RedisKeyConstants.REFRESH_TOKEN_USER_CLIENT_PREFIX
                + userId + ":" + clientType + ":*";
        Set<String> clientIndexKeys = stringRedisTemplate.keys(pattern);
        if (clientIndexKeys == null || clientIndexKeys.isEmpty()) {
            return;
        }
        for (String clientIndexKey : clientIndexKeys) {
            String hash = clientIndexKey.substring(clientIndexKey.lastIndexOf(":") + 1);
            String checkKey = RedisKeyConstants.REFRESH_TOKEN_PREFIX + hash;
            String userIndexKey = RedisKeyConstants.REFRESH_TOKEN_USER_PREFIX + userId + ":" + hash;
            stringRedisTemplate.delete(checkKey);
            stringRedisTemplate.delete(userIndexKey);
        }
        stringRedisTemplate.delete(clientIndexKeys);
        log.info("Cleared refresh tokens by clientType: userId={}, clientType={}, count={}",
                userId, clientType, clientIndexKeys.size());
    }

    /**
     * SHA-256哈希工具方法
     *
     * @param input 输入字符串
     * @return 十六进制哈希值
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
