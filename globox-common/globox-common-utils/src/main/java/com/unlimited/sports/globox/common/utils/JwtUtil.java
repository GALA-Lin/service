package com.unlimited.sports.globox.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类（网关/用户服务共用）
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
public class JwtUtil {

    /**
     * 生成JWT Token
     *
     * @param subject       主题（通常是userId）
     * @param claims        自定义声明（如角色、权限等）
     * @param secret        密钥
     * @param expireSeconds 过期时间（秒）
     * @return JWT Token字符串
     */
    public static String generateToken(String subject, Map<String, Object> claims, String secret, long expireSeconds) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expireSeconds * 1000);

        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(key)
                .compact();
    }

    /**
     * 解析JWT Token
     *
     * @param token  JWT Token字符串
     * @param secret 密钥
     * @return Claims对象（包含所有声明）
     * @throws io.jsonwebtoken.JwtException 如果Token无效或过期
     */
    public static Claims parseToken(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从Token中获取主题（userId）
     *
     * @param token  JWT Token字符串
     * @param secret 密钥
     * @return 主题字符串
     */
    public static String getSubject(String token, String secret) {
        return parseToken(token, secret).getSubject();
    }

    /**
     * 从Token中获取指定声明
     *
     * @param token  JWT Token字符串
     * @param secret 密钥
     * @param key    声明的key
     * @param clazz  声明值的类型
     * @return 声明值
     */
    public static <T> T getClaim(String token, String secret, String key, Class<T> clazz) {
        Claims claims = parseToken(token, secret);
        return claims.get(key, clazz);
    }

    /**
     * 验证Token是否有效（未过期且签名正确）
     *
     * @param token  JWT Token字符串
     * @param secret 密钥
     * @return true=有效，false=无效
     */
    public static boolean validateToken(String token, String secret) {
        try {
            parseToken(token, secret);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查Token是否过期
     *
     * @param token  JWT Token字符串
     * @param secret 密钥
     * @return true=已过期，false=未过期
     */
    public static boolean isTokenExpired(String token, String secret) {
        try {
            Claims claims = parseToken(token, secret);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 获取Token的剩余有效时间（秒）
     *
     * @param token  JWT Token字符串
     * @param secret 密钥
     * @return 剩余秒数，如果已过期或解析失败返回0
     * 目前不需要调用,可保留方法,后续token时间出现问题如时长过短,可调用此方法打日志查看剩余时长排查问题
     */
    public static long getRemainingSeconds(String token, String secret) {
        try {
            Claims claims = parseToken(token, secret);
            Date expiration = claims.getExpiration();
            long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(0, remaining);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 从Authorization请求头中提取Token
     * 格式：Bearer <token> 或直接是 <token>
     *
     * @param authHeader Authorization请求头的值
     * @return Token字符串，如果格式不正确返回null
     */
    public static String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || authHeader.trim().isEmpty()) {
            return null;
        }
        // 去除Bearer前缀（不区分大小写）
        String token = authHeader.trim();
        if (token.length() > 7 && token.substring(0, 7).equalsIgnoreCase("Bearer ")) {
            token = token.substring(7).trim();
        }
        return token.isEmpty() ? null : token;
    }
}

