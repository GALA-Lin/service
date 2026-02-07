package com.unlimited.sports.globox.user.service;

/**
 * 短信服务接口
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
import com.unlimited.sports.globox.model.auth.enums.SmsScene;

public interface SmsService {

    /**
     * 生成6位随机验证码
     *
     * @return 验证码字符串
     */
    String generateCode();

    /**
     * 发送验证码短信
     *
     * @param phone 手机号
     * @param code  验证码
     * @return true=发送成功，false=发送失败
     */
    /**
     * 发送验证码短信（按场景）
     *
     * @param phone 手机号
     * @param code  验证码
     * @param scene 场景
     * @return true=发送成功，false=发送失败
     */
    boolean sendCode(String phone, String code, SmsScene scene);

    /**
     * 发送登录成功通知
     *
     * @param phone 手机号
     * @return true=发送成功，false=发送失败
     */
    boolean sendLoginNotice(String phone);
}
