package com.unlimited.sports.globox.user.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码工具类
 * 使用BCrypt进行密码加密和验证
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
public class PasswordUtils {

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 密码强度校验（6-20位）
     *
     * @param password 密码
     * @return true=符合要求，false=不符合要求
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        int length = password.length();
        return length >= 6 && length <= 20;
    }

    /**
     * BCrypt加密密码
     *
     * @param rawPassword 原始密码
     * @return 加密后的密码hash
     */
    public static String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * BCrypt验证密码
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密码hash
     * @return true=匹配，false=不匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
