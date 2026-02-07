package com.unlimited.sports.globox.common.utils;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 生成随机数字工具类
 */
public final class RandomNumberUtils {

    private RandomNumberUtils() {}

    /**
     * 生成 n 位纯数字字符串（首位不为 0）
     * 例如 n=1 => "0"~"9"
     *     n=2 => "10"~"99"
     *     n=8 => "10000000"~"99999999"
     */
    public static String randomDigits(int digits) {
        if (digits <= 0) {
            throw new IllegalArgumentException("digits must be > 0");
        }
        if (digits == 1) {
            return String.valueOf(ThreadLocalRandom.current().nextInt(10));
        }

        // 首位 1~9
        StringBuilder sb = new StringBuilder(digits);
        sb.append(ThreadLocalRandom.current().nextInt(1, 10));

        // 后续位 0~9
        for (int i = 1; i < digits; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 生成 n 位纯数字字符串（首位不为 0），使用 SecureRandom（更安全）
     */
    public static String randomDigitsSecure(int digits) {
        if (digits <= 0) {
            throw new IllegalArgumentException("digits must be > 0");
        }
        SecureRandom r = Holder.SECURE_RANDOM;

        if (digits == 1) {
            return String.valueOf(r.nextInt(10));
        }

        StringBuilder sb = new StringBuilder(digits);
        sb.append(r.nextInt(9) + 1); // 1~9
        for (int i = 1; i < digits; i++) {
            sb.append(r.nextInt(10)); // 0~9
        }
        return sb.toString();
    }

    /**
     * 生成 n 位纯数字，并返回 long（仅适用于 digits <= 18，且首位不为 0）
     */
    public static long randomDigitsAsLong(int digits) {
        if (digits <= 0 || digits > 18) {
            throw new IllegalArgumentException("digits must be between 1 and 18 for long");
        }
        return Long.parseLong(randomDigits(digits));
    }

    /**
     * 生成 32 位无符号整数范围 [0, 2^32-1]，用 long 承载
     */
    public static long randomUInt32() {
        return ThreadLocalRandom.current().nextLong(0L, 1L << 32); // [0, 2^32)
    }

    /**
     * 生成 32 位无符号整数范围 [0, 2^32-1]（更安全）
     */
    public static long randomUInt32Secure() {
        return (Holder.SECURE_RANDOM.nextInt() & 0xFFFF_FFFFL);
    }

    private static final class Holder {
        private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    }
}