package com.unlimited.sports.globox.user.util;

/**
 * PhoneUtils
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
public class PhoneUtils {
    
    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
    
    /**
     * 验证手机号格式
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        return phone.matches(PHONE_REGEX);
    }
    
    /**
     * 手机号脱敏
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
    
    /**
     * 获取手机号后4位（用于JWT存储，避免泄露完整手机号）
     * 示例：13800138000 -> 8000
     * 
     * @param phone 完整手机号
     * @return 后4位，如果手机号长度不足4位则返回空字符串
     */
    public static String getPhoneSuffix(String phone) {
        if (phone == null || phone.length() < 4) {
            return "";
        }
        return phone.substring(phone.length() - 4);
    }
}
