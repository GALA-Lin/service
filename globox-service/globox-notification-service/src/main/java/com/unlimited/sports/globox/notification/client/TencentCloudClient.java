package com.unlimited.sports.globox.notification.client;

import com.alibaba.fastjson2.JSON;
import com.tencent.xinge.XingeApp;
import com.tencent.xinge.bean.*;
import com.tencent.xinge.bean.ios.Alert;
import com.tencent.xinge.bean.ios.Aps;
import com.tencent.xinge.push.app.PushAppRequest;
import com.unlimited.sports.globox.notification.config.TencentCloudProperties;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 腾讯云TPNS推送客户端
 * 负责与腾讯云API交互，发送推送通知
 */
@Slf4j
@Component
public class TencentCloudClient {

    private final TencentCloudProperties properties;
    private XingeApp xingeApp;




    public TencentCloudClient(TencentCloudProperties properties,@Value("${tencent.cloud.tpns.app-id}") String appId) {
        log.info("appId{}",appId);
        this.properties = properties;
        this.initClient();
    }

    /**
     * 初始化TPNS客户端
     */
    private void initClient() {
        try {
            log.info("初始化腾讯云TPNS客户端{}",properties);
            // 优先使用新属性名，兼容旧属性名
            String appId = properties.getAppId();
            String secretKey = properties.getSecretKey() != null ? properties.getSecretKey() : properties.getAccessKey();
            String domainUrl = properties.getDomainUrl() != null ? properties.getDomainUrl() :
                             (properties.getServiceUrl() != null ? properties.getServiceUrl() : "https://api.tpns.tencent.com/");

            if (appId == null || appId.isEmpty()) {
                throw new RuntimeException("腾讯云AppId未配置");
            }
            if (secretKey == null || secretKey.isEmpty()) {
                throw new RuntimeException("腾讯云SecretKey未配置");
            }

            this.xingeApp = new XingeApp.Builder()
                    .appId(appId)
                    .secretKey(secretKey)
                    .domainUrl(domainUrl)
                    .build();

            log.info("腾讯云TPNS客户端初始化成功: appId={}", appId);

        } catch (Exception e) {
            log.error("腾讯云TPNS客户端初始化失败", e);
            throw new RuntimeException("腾讯云TPNS客户端初始化失败", e);
        }
    }


    /**
     * 发送单个推送（按注册ID）
     *
     * @param deviceToken 设备token（腾讯云registrationId）
     * @param title 推送标题
     * @param content 推送内容
     * @param action deeplink，如tennis://order/123
     * @param customData 自定义数据
     * @return 腾讯云返回的push_id，失败返回null
     */
    public String pushToSingleDevice(String deviceToken, String title, String content,
                                     String action, Map<String, Object> customData) {
        try {
            if (deviceToken == null || deviceToken.isEmpty()) {
                log.warn("设备token为空");
                return null;
            }

            log.info("发送推送到单个设备: deviceToken={}, title={}", deviceToken, title);

            // 创建推送请求
            PushAppRequest pushAppRequest = new PushAppRequest();

            // 设置推送类型和平台
            pushAppRequest.setAudience_type(AudienceType.token);
            pushAppRequest.setPlatform(Platform.all);
            pushAppRequest.setMessage_type(MessageType.notify);

            // 设置token列表
            ArrayList<String> tokenList = new ArrayList<>();
            tokenList.add(deviceToken);
            pushAppRequest.setToken_list(tokenList);

            // 设置推送内容
            setMessageContent(pushAppRequest, title, content, action, customData);

            // 调用腾讯云API
            JSONObject response = xingeApp.pushApp(pushAppRequest);

            if (response != null && response.getInt("ret_code") == 0) {
                JSONObject resultObj = response.optJSONObject("result");
                if (resultObj != null) {
                    String pushId = resultObj.optString("push_id");
                    log.info("推送已提交到腾讯云: deviceToken={}, pushId={}", deviceToken, pushId);
                    return pushId;
                }
            }

            String errMsg = response != null ? response.optString("err_msg", "unknown error") : "unknown error";
            log.error("发送推送失败: deviceToken={}, 错误信息={}", deviceToken, errMsg);
            return null;

        } catch (Exception e) {
            log.error("发送推送失败: deviceToken={}", deviceToken, e);
            return null;
        }
    }

    /**
     * 设置消息内容（Android + iOS）
     */
    private void setMessageContent(PushAppRequest request, String title, String content,
                                   String action, Map<String, Object> customData) {
        Message message = new Message();
        message.setTitle(title);
        message.setContent(content);

        // 设置Android消息（简化处理，官方示例未展示自定义数据设置）
        MessageAndroid messageAndroid = new MessageAndroid();
        message.setAndroid(messageAndroid);

        // 设置iOS消息（官方SDK示例未展示自定义数据，简化处理）
        MessageIOS messageIOS = new MessageIOS();
        Alert alert = new Alert();
        alert.setTitle(title);
        alert.setBody(content);

        Aps aps = new Aps();
        aps.setAlert(alert);
        messageIOS.setAps(aps);

        message.setIos(messageIOS);

        request.setMessage(message);
    }

    /**
     * 按用户ID发送推送（推送给该用户的所有活跃设备）
     *
     * @param userId 用户ID
     * @param title 推送标题
     * @param content 推送内容
     * @param action deeplink
     * @param customData 自定义数据
     * @return 腾讯云返回的push_id，失败返回null
     */
    public String pushToUser(Long userId, String title, String content,
                            String action, Map<String, Object> customData) {
        try {
            log.info("发送推送到用户: userId={}, title={}", userId, title);

            // 创建推送请求
            PushAppRequest pushAppRequest = new PushAppRequest();

            // 设置推送类型和平台
            pushAppRequest.setAudience_type(AudienceType.account);
            pushAppRequest.setPlatform(Platform.all);
            pushAppRequest.setMessage_type(MessageType.notify);
            pushAppRequest.setAccount_type(1);
            pushAppRequest.setAccount_push_type(1);

            // 设置账号列表
            ArrayList<String> accountList = new ArrayList<>();
            accountList.add(String.valueOf(userId));
            pushAppRequest.setAccount_list(accountList);

            // 设置推送内容
            setMessageContent(pushAppRequest, title, content, action, customData);

            // 调用腾讯云API
            JSONObject response = xingeApp.pushApp(pushAppRequest);

            if (response != null && response.getInt("ret_code") == 0) {
                JSONObject resultObj = response.optJSONObject("result");
                if (resultObj != null) {
                    String pushId = resultObj.optString("push_id");
                    log.info("推送已提交到腾讯云: userId={}, pushId={}", userId, pushId);
                    return pushId;
                }
            }

            String errMsg = response != null ? response.optString("err_msg", "unknown error") : "unknown error";
            log.error("发送推送失败: userId={}, 错误信息={}", userId, errMsg);
            return null;

        } catch (Exception e) {
            log.error("发送推送失败: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 批量发送推送
     *
     * @param deviceTokens 设备token列表
     * @param title 推送标题
     * @param content 推送内容
     * @param action deeplink
     * @param customData 自定义数据
     * @return 腾讯云返回的push_id，失败返回null
     */
    public String batchPush(List<String> deviceTokens, String title, String content,
                           String action, Map<String, Object> customData) {
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            log.warn("设备token列表为空");
            return null;
        }

        try {
            log.info("批量发送推送: 设备数量={}, title={}", deviceTokens.size(), title);

            // 创建推送请求
            PushAppRequest pushAppRequest = new PushAppRequest();

            // 设置推送类型和平台
            pushAppRequest.setAudience_type(AudienceType.token_list);
            pushAppRequest.setPlatform(Platform.all);
            pushAppRequest.setMessage_type(MessageType.notify);

            // 设置token列表
            ArrayList<String> tokenList = new ArrayList<>(deviceTokens);
            pushAppRequest.setToken_list(tokenList);

            // 设置推送内容
            setMessageContent(pushAppRequest, title, content, action, customData);

            // 调用腾讯云API
            JSONObject response = xingeApp.pushApp(pushAppRequest);

            if (response != null && response.getInt("ret_code") == 0) {
                JSONObject resultObj = response.optJSONObject("result");
                if (resultObj != null) {
                    String pushId = resultObj.optString("push_id");
                    log.info("批量推送已提交到腾讯云: 设备数量={}, pushId={}", deviceTokens.size(), pushId);
                    return pushId;
                }
            }

            String errMsg = response != null ? response.optString("err_msg", "unknown error") : "unknown error";
            log.error("批量发送推送失败: 设备数量={}, 错误信息={}", deviceTokens.size(), errMsg);
            return null;

        } catch (Exception e) {
            log.error("批量发送推送失败: 设备数量={}", deviceTokens.size(), e);
            return null;
        }
    }



}
