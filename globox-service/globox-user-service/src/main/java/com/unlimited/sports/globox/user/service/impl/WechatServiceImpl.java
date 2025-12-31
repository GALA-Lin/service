package com.unlimited.sports.globox.user.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.model.auth.vo.WechatUserInfo;
import com.unlimited.sports.globox.user.service.WechatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

/**
 * 微信服务实现类
 *
 * @author Wreckloud
 * @since 2025/12/20
 */
@Service
@Slf4j
public class WechatServiceImpl implements WechatService {

    /**
     * 强制Mock模式开关（优先级最高）
     * 如果设置为true，无论配置是否齐全都使用Mock模式
     */
    @Value("${wechat.mock-enabled:false}")
    private boolean forceMockEnabled;

    /**
     * 微信服务开关：关闭时强制使用Mock模式
     */
    @Value("${wechat.enabled:true}")
    private boolean wechatEnabled;

    /**
     * 微信API URL
     */
    @Value("${wechat.api-url:https://api.weixin.qq.com/sns/jscode2session}")
    private String wechatApiUrl;

    /**
     * 微信配置
     */
    @Value("${wechat.app-id:}")
    private String appId;

    @Value("${wechat.app-secret:}")
    private String appSecret;

    /**
     * 是否使用Mock模式（自动检测结果）
     */
    private boolean useMockMode;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 初始化：检测配置并决定使用Mock模式还是真实微信API模式
     */
    @PostConstruct
    public void init() {
        if (!wechatEnabled) {
            useMockMode = true;
            log.warn("【微信服务】wechat.enabled=false，强制使用Mock模式");
            return;
        }

        // 如果强制启用Mock模式，直接使用Mock
        if (forceMockEnabled) {
            useMockMode = true;
            log.info("【微信服务】强制启用Mock模式（wechat.mock-enabled=true）");
            return;
        }

        // 检测微信配置是否齐全
        boolean configComplete = StringUtils.hasText(appId) && StringUtils.hasText(appSecret);

        if (configComplete) {
            useMockMode = false;
            log.info("【微信服务】检测到微信配置齐全，使用真实微信API模式");
        } else {
            useMockMode = true;
            log.warn("【微信服务】微信配置不完整，自动启用Mock模式");
            log.warn("【微信服务】缺少的配置项：app-id={}, app-secret={}",
                    StringUtils.hasText(appId) ? "已配置" : "未配置",
                    StringUtils.hasText(appSecret) ? "已配置" : "未配置");
        }
    }

    @Override
    public WechatUserInfo getOpenIdAndUnionId(String code) {
        // 1) 基础校验
        if (!StringUtils.hasText(code)) {
            log.error("【微信服务】授权码为空");
            throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
        }

        // 2) Mock模式（本地联调用）
        if (useMockMode) {
            WechatUserInfo mock = new WechatUserInfo();
            // 使用code生成固定的mock openid和unionid，便于测试
            mock.setOpenid("mock-openid-" + code);
            mock.setUnionid("mock-unionid-" + code);
            mock.setSessionKey("mock-session-key");
            log.info("【Mock微信】登录成功：code={}, openid={}, unionid={}", 
                    code, mock.getOpenid(), mock.getUnionid());
            return mock;
        }

        // 3) 真实微信API模式
        // 配置校验（理论上不会走到这里，因为init()已经检测过了，但保留作为双重保险）
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            log.error("【微信服务】配置缺失：app-id或app-secret为空");
            throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
        }

        // 4) 构建请求
        String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                wechatApiUrl, appId, appSecret, code);

        try {
            if (restTemplate == null) {
                restTemplate = new RestTemplate();
            }
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            String responseBody = response.getBody();
            if (!StringUtils.hasText(responseBody)) {
                log.error("【微信API】响应为空：code={}", code);
                throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
            }

            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // 5) 错误处理
            if (jsonNode.has("errcode")) {
                int errcode = jsonNode.get("errcode").asInt();
                String errmsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : "unknown error";

                log.error("【微信API】请求失败：code={}, errcode={}, errmsg={}", code, errcode, errmsg);

                if (errcode == 40029 || errcode == 40163) {
                    throw new GloboxApplicationException(UserAuthCode.WECHAT_CODE_EXPIRED);
                } else {
                    throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
                }
            }

            // 6) 提取数据
            if (!jsonNode.has("openid")) {
                log.error("【微信API】响应缺少openid：code={}, response={}", code, responseBody);
                throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
            }

            WechatUserInfo userInfo = new WechatUserInfo();
            userInfo.setOpenid(jsonNode.get("openid").asText());
            userInfo.setSessionKey(jsonNode.has("session_key") ? jsonNode.get("session_key").asText() : null);
            userInfo.setUnionid(jsonNode.has("unionid") && !jsonNode.get("unionid").isNull()
                    ? jsonNode.get("unionid").asText() : null);

            log.info("【微信API】登录成功：code={}, openid={}, hasUnionid={}",
                    code, userInfo.getOpenid(), userInfo.getUnionid() != null);

            return userInfo;

        } catch (GloboxApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("【微信API】请求异常：code={}, error={}", code, e.getMessage(), e);
            throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
        }
    }
}
