package com.unlimited.sports.globox.user.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.model.auth.vo.WechatUserInfo;
import com.unlimited.sports.globox.user.config.WechatProperties;
import com.unlimited.sports.globox.user.service.WechatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信服务实现类
 *
 * @author Wreckloud
 * @since 2025/12/20
 */
@Service
@Slf4j
public class WechatServiceImpl implements WechatService {

    @Autowired
    private WechatProperties wechatProperties;

    @Autowired
    private Environment environment;

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
        if (wechatProperties.getEnabled() == null || !wechatProperties.getEnabled()) {
            useMockMode = true;
            log.warn("【微信服务】wechat.enabled=false，强制使用Mock模式");
            return;
        }

        // 如果强制启用Mock模式，直接使用Mock
        if (wechatProperties.getMockEnabled() != null && wechatProperties.getMockEnabled()) {
            useMockMode = true;
            log.info("【微信服务】强制启用Mock模式（wechat.mock-enabled=true）");
            return;
        }

        // 非Mock模式下，严格依赖配置，缺失时在请求阶段直接报错
        useMockMode = false;
    }

    /**
     * 根据客户端类型解析微信配置
     *
     * @param clientType 客户端类型
     * @return 微信配置（MiniappConfig 或 UniappConfig）
     */
    private WechatProperties.MiniappConfig resolveWechatConfig(String clientType) {
        if (!StringUtils.hasText(clientType)) {
            log.error("【微信服务】clientType为空");
            throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
        }

        switch (clientType) {
            case "third-party-jsapi":
                WechatProperties.MiniappConfig miniapp = wechatProperties.getMiniapp();
                if (miniapp == null || !StringUtils.hasText(miniapp.getAppId())
                        || !StringUtils.hasText(miniapp.getAppSecret())) {
                    log.error("【微信服务】miniapp配置不完整：app-id或app-secret为空");
                    throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
                }
                return miniapp;
            case "app":
                WechatProperties.UniappConfig uniapp = wechatProperties.getUniapp();
                if (uniapp == null || !StringUtils.hasText(uniapp.getAppId())
                        || !StringUtils.hasText(uniapp.getAppSecret())) {
                    log.error("【微信服务】uniapp配置不完整：app-id或app-secret为空");
                    throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
                }
                // 返回 MiniappConfig 类型，但实际使用 uniapp 的配置
                // 由于 MiniappConfig 和 UniappConfig 结构相同，这里创建一个临时对象
                WechatProperties.MiniappConfig uniappAsMiniapp = new WechatProperties.MiniappConfig();
                uniappAsMiniapp.setApiUrl(uniapp.getApiUrl());
                uniappAsMiniapp.setAppId(uniapp.getAppId());
                uniappAsMiniapp.setAppSecret(uniapp.getAppSecret());
                return uniappAsMiniapp;
            default:
                log.error("【微信服务】不支持的clientType：{}，仅支持 third-party-jsapi 和 app", clientType);
                throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
        }
    }

    @Override
    public WechatUserInfo getOpenIdAndUnionId(String code, String clientType) {
        // 1) 基础校验
        log.info("wechat log code :{}",code);
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
            log.info("【Mock微信】登录成功：code={}, clientType={}", code, clientType);
            return mock;
        }

        // 3) 根据clientType解析配置
        WechatProperties.MiniappConfig config = resolveWechatConfig(clientType);
        String apiUrl = StringUtils.hasText(config.getApiUrl()) 
                ? config.getApiUrl() 
                : "https://api.weixin.qq.com/sns/jscode2session";
        String appId = config.getAppId();
        String appSecret = config.getAppSecret();

        // 4) 构建请求
        String url;
        ClientType clientTypeEnum = ClientType.fromValue(clientType);
        if (clientTypeEnum.equals(ClientType.APP)) {
            url = String.format("%s?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
                    apiUrl, appId, appSecret, code);
        } else {
            url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    apiUrl, appId, appSecret, code);
        }


        log.info("url:{}", url);

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

            log.debug("【微信API】jscode2session响应：code={}, response={}", code, responseBody);
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

            log.info("【微信API】登录成功：code={}, clientType={}, hasUnionid={}",
                    code, clientType, userInfo.getUnionid() != null);

            return userInfo;

        } catch (GloboxApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("【微信API】请求异常：code={}, error={}", code, e.getMessage(), e);
            throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
        }
    }

    @Override
    public String getPhoneNumber(String wxCode, String phoneCode, String clientType) {
        // 1) 基础校验
        if (!StringUtils.hasText(wxCode) || !StringUtils.hasText(phoneCode)) {
            log.error("【微信服务】wxCode或phoneCode为空");
            throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
        }

        // 2) Mock模式（本地联调用）
        if (useMockMode) {
            String mockPhone = "13800138000";
            log.info("【Mock微信】获取手机号成功：wxCode={}, phoneCode={}, clientType={}, phone={}", 
                    wxCode, phoneCode, clientType, mockPhone);
            return mockPhone;
        }

        // 3) 根据clientType解析配置（严格模式：jsapi等不支持的类型会直接报错）
        WechatProperties.MiniappConfig config = resolveWechatConfig(clientType);

        try {
            // 4) 获取access_token
            String accessToken = getAccessToken(config.getAppId(), config.getAppSecret());
            if (!StringUtils.hasText(accessToken)) {
                log.error("【微信服务】获取access_token失败");
                throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
            }

            // 5) 调用getuserphonenumber API
            String phoneApiUrl = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + accessToken;
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("code", phoneCode);

            if (restTemplate == null) {
                restTemplate = new RestTemplate();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    phoneApiUrl, HttpMethod.POST, requestEntity, String.class);

            String responseBody = response.getBody();
            if (!StringUtils.hasText(responseBody)) {
                log.error("【微信API】获取手机号响应为空：phoneCode={}", phoneCode);
                throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
            }

            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // 6) 错误处理：errcode 为 0 或不存在视为成功
            if (jsonNode.has("errcode")) {
                int errcode = jsonNode.get("errcode").asInt();
                if (errcode != 0) {
                    String errmsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : "unknown error";
                    log.error("【微信API】获取手机号失败：phoneCode={}, errcode={}, errmsg={}", phoneCode, errcode, errmsg);
                    if (errcode == 40029 || errcode == 40163) {
                        throw new GloboxApplicationException(UserAuthCode.WECHAT_CODE_EXPIRED);
                    }
                    throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
                }
            }

            // 7) 提取手机号
            if (!jsonNode.has("phone_info")) {
                log.error("【微信API】响应缺少phone_info：phoneCode={}, response={}", phoneCode, responseBody);
                throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
            }

            JsonNode phoneInfo = jsonNode.get("phone_info");
            if (!phoneInfo.has("phoneNumber")) {
                log.error("【微信API】phone_info缺少phoneNumber：phoneCode={}, response={}", phoneCode, responseBody);
                throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
            }

            // 微信API直接返回明文手机号（新版本API）
            String phoneNumber = phoneInfo.get("phoneNumber").asText();

            log.info("【微信API】获取手机号成功：phoneCode={}, phone={}", phoneCode, phoneNumber);
            return phoneNumber;

        } catch (GloboxApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("【微信API】获取手机号异常：wxCode={}, phoneCode={}, error={}", wxCode, phoneCode, e.getMessage(), e);
            throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
        }
    }

    /**
     * 获取微信access_token
     * 注意：实际生产环境应该缓存access_token，避免频繁调用
     *
     * @param appId 微信AppId
     * @param appSecret 微信AppSecret
     * @return access_token
     */
    private String getAccessToken(String appId, String appSecret) {
        try {
            String url = String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                    appId, appSecret);

            if (restTemplate == null) {
                restTemplate = new RestTemplate();
            }

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String responseBody = response.getBody();

            if (!StringUtils.hasText(responseBody)) {
                log.error("【微信API】获取access_token响应为空");
                return null;
            }

            JsonNode jsonNode = objectMapper.readTree(responseBody);

            if (jsonNode.has("errcode")) {
                int errcode = jsonNode.get("errcode").asInt();
                String errmsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : "unknown error";
                log.error("【微信API】获取access_token失败：errcode={}, errmsg={}", errcode, errmsg);
                return null;
            }

            if (!jsonNode.has("access_token")) {
                log.error("【微信API】响应缺少access_token：response={}", responseBody);
                return null;
            }

            String accessToken = jsonNode.get("access_token").asText();
            log.info("【微信API】获取access_token成功");
            return accessToken;

        } catch (Exception e) {
            log.error("【微信API】获取access_token异常：error={}", e.getMessage(), e);
            return null;
        }
    }
}
