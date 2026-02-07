package com.unlimited.sports.globox.user.service.impl;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.unlimited.sports.globox.model.auth.enums.SmsScene;
import com.unlimited.sports.globox.user.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Random;

/**
 * 短信服务实现
 * 
 * 说明：
 * 1. 验证码生成逻辑统一，无论真实短信还是Mock模式都使用同一个方法
 * 2. 自动检测腾讯云配置是否齐全，配置不完整时自动使用Mock模式
 * 3. 可通过 sms.mock-enabled 强制启用Mock模式（优先级最高）
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    /**
     * 强制Mock模式开关（优先级最高）
     * 如果设置为true，无论配置是否齐全都使用Mock模式
     */
    @Value("${sms.mock-enabled:false}")
    private boolean forceMockEnabled;

    /**
     * 腾讯云短信配置
     */
    @Value("${sms.tencent.secret-id:}")
    private String secretId;

    @Value("${sms.tencent.secret-key:}")
    private String secretKey;

    @Value("${sms.tencent.sms-sdk-app-id:}")
    private String smsSdkAppId;

    @Value("${sms.tencent.sign-name:球盒App}")
    private String signName;

    @Value("${sms.tencent.template-id:}")
    private String templateId;

    @Value("${sms.tencent.template-id-login:}")
    private String loginTemplateId;

    @Value("${sms.tencent.template-id-cancel:}")
    private String cancelTemplateId;

    @Value("${sms.tencent.template-id-bind:}")
    private String bindTemplateId;

    /**
     * 模板中有效期占位符（分钟），默认5
     */
    @Value("${sms.tencent.expire-minutes:5}")
    private int expireMinutes;
    /**
     * 是否使用Mock模式（自动检测结果）
     */
    private boolean useMockMode;

    /**
     * 初始化：检测配置并决定使用Mock模式还是真实短信模式
     */
    @PostConstruct
    public void init() {
        // 如果强制启用Mock模式，直接使用Mock
        if (forceMockEnabled) {
            useMockMode = true;
            log.info("【短信服务】强制启用Mock模式（sms.mock-enabled=true）");
            return;
        }

        // 检测腾讯云配置是否齐全
        boolean templateConfigured = StringUtils.hasText(loginTemplateId)
                || StringUtils.hasText(cancelTemplateId)
                || StringUtils.hasText(bindTemplateId)
                || StringUtils.hasText(templateId);

        boolean configComplete = StringUtils.hasText(secretId) 
                && StringUtils.hasText(secretKey)
                && StringUtils.hasText(smsSdkAppId) 
                && templateConfigured;

        if (configComplete) {
            useMockMode = false;
            log.info("【短信服务】检测到腾讯云配置齐全，使用真实短信模式");
        } else {
            useMockMode = true;
            log.warn("【短信服务】腾讯云配置不完整，自动启用Mock模式");
            log.warn("【短信服务】缺少的配置项：secret-id={}, secret-key={}, sms-sdk-app-id={}, template-id={}", 
                    StringUtils.hasText(secretId) ? "已配置" : "未配置",
                    StringUtils.hasText(secretKey) ? "已配置" : "未配置",
                    StringUtils.hasText(smsSdkAppId) ? "已配置" : "未配置",
                    StringUtils.hasText(templateId) ? "已配置" : "未配置");
        }
    }

    /**
     * 生成6位随机验证码
     * 统一验证码生成逻辑，无论真实短信还是Mock模式都使用此方法
     *
     * @return 验证码字符串（100000-999999）
     */
    @Override
    public String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 发送验证码短信
     * 统一发送入口，内部根据配置自动选择Mock模式或真实短信模式
     *
     * @param phone 手机号
     * @param code  验证码（由generateCode()生成）
     * @return true=发送成功，false=发送失败
     */
    @Override
    public boolean sendCode(String phone, String code, SmsScene scene) {
        String sceneTemplateId = resolveTemplateId(scene);
        if (useMockMode) {
            return sendMockSms(phone, code, scene);
        } else {
            return sendRealSms(phone, code, sceneTemplateId);
        }
    }

    @Override
    public boolean sendLoginNotice(String phone) {
        if (useMockMode) {
            log.info("【Mock短信】登录成功通知已发送到：{}", phone);
            return true;
        }
        // 真实模式下暂不支持登录通知
        return false;
    }

    /**
     * Mock模式：控制台打印验证码
     */
    private boolean sendMockSms(String phone, String code, SmsScene scene) {
        log.info("=================================================");
        log.info("【Mock短信】发送验证码，场景={}", scene);
        log.info("手机号：{}", phone);
        log.info("验证码：{}", code);
        log.info("有效期：5分钟");
        log.info("=================================================");
        return true;
    }

    /**
     * 真实短信模式：调用腾讯云短信服务发送
     */
    private boolean sendRealSms(String phone, String code, String sceneTemplateId) {
        try {
            // 1. 实例化认证对象
            Credential cred = new Credential(secretId, secretKey);

            // 2. 实例化HTTP选项
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("sms.tencentcloudapi.com");

            // 3. 实例化客户端配置
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            // 4. 实例化SMS客户端
            SmsClient client = new SmsClient(cred, "ap-beijing", clientProfile);

            // 5. 实例化请求对象
            SendSmsRequest req = new SendSmsRequest();
            
            // 设置短信应用ID
            req.setSmsSdkAppId(smsSdkAppId);
            
            // 设置签名名称
            req.setSignName(signName);
            
            // 设置模板ID
            req.setTemplateId(sceneTemplateId);
            
            // 设置模板参数：{1}=验证码, {2}=有效期(分钟)
            String[] templateParamSet = {code, String.valueOf(expireMinutes)};
            req.setTemplateParamSet(templateParamSet);
            
            // 设置手机号（需要带国家码，如：+86）
            String[] phoneNumberSet = {"+86" + phone};
            req.setPhoneNumberSet(phoneNumberSet);

            // 6. 发送请求
            SendSmsResponse resp = client.SendSms(req);

            // 7. 检查发送结果
            if (resp.getSendStatusSet() != null && resp.getSendStatusSet().length > 0) {
                String sendStatus = resp.getSendStatusSet()[0].getCode();
                String message = resp.getSendStatusSet()[0].getMessage();
                
                if ("Ok".equals(sendStatus)) {
                    log.info("【腾讯云短信】发送成功：phone={}, code={}", phone, code);
                    return true;
                } else {
                    log.error("【腾讯云短信】发送失败：phone={}, code={}, error={}, message={}", 
                            phone, code, sendStatus, message);
                    return false;
                }
            } else {
                log.error("【腾讯云短信】发送失败：响应为空");
                return false;
            }

        } catch (TencentCloudSDKException e) {
            log.error("【腾讯云短信】发送异常：phone={}, code={}, error={}", 
                    phone, code, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("【短信服务】系统异常：phone={}, code={}", phone, code, e);
            return false;
        }
    }

    private String resolveTemplateId(SmsScene scene) {
        String loginId = StringUtils.hasText(loginTemplateId)
                ? loginTemplateId
                : (StringUtils.hasText(templateId) ? templateId : cancelTemplateId);
        String cancelId = StringUtils.hasText(cancelTemplateId) ? cancelTemplateId : loginId;
        String bindId = StringUtils.hasText(bindTemplateId) ? bindTemplateId : loginId;
        if (scene == SmsScene.CANCEL) {
            return cancelId;
        }
        if (scene == SmsScene.BIND) {
            return bindId;
        }
        return loginId;
    }
}
