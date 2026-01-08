package com.unlimited.sports.globox.social.util;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.tencentyun.TLSSigAPIv2;
import com.unlimited.sports.globox.model.social.entity.TencentCloudImApiEnum;
import com.unlimited.sports.globox.model.social.entity.TencentCloudImConstantEnum;
import com.unlimited.sports.globox.model.social.entity.TencentImResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author : yc
 * @since : 2025-12-21
 **/
@Slf4j
@Component
public class TencentCloudImUtil {
    private static final String HTTPS_URL_PREFIX = "https://console.tim.qq.com/";
    private static final String APP_MANAGER = "administrator";
    private static final String REDIS_IM_USER_SIG = "silence:test_im_user_sig:";
 
    @Value("${IMConfig.sdkAppId}")
    private long sdkAppId;
    @Value("${IMConfig.secretKey}")
    private String secretKey;

    private final RedisTemplate redisTemplate;

    public TencentCloudImUtil(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

 
    /**
     * 获取腾讯云用户签名
     */
    public String getTxCloudUserSig(String UserId) {
        String key = REDIS_IM_USER_SIG + UserId;
        String userSig = (String) redisTemplate.opsForValue().get(key);
        if (StringUtils.isEmpty(userSig)) {
            // 如果Redis中没有，则重新生成
            TLSSigAPIv2 tlsSigApi = new TLSSigAPIv2(sdkAppId, secretKey);
            userSig = tlsSigApi.genUserSig(UserId, 86400);

            // 将新生成的签名存入Redis，有效期为1天
            redisTemplate.opsForValue().set(key, userSig, 86400, TimeUnit.SECONDS);
        }

        return userSig;
    }
 
    /**
     * 获取腾讯im请求路径
     */
    private String getHttpsUrl(String imServiceApi, Integer random) {
        return String.format("%s%s?sdkappid=%s&identifier=%s&usersig=%s&random=%s&contenttype=json",
                HTTPS_URL_PREFIX, imServiceApi, sdkAppId, APP_MANAGER, this.getTxCloudUserSig(APP_MANAGER), random);
    }
 
    /**
     * 导入单个账号
     * @param userId 用户id
     */
    public void accountImport(String userId) {
        accountImport(userId, null);
    }
 
    public void accountImport(String userId, String userName) {
        accountImport(userId, userName, null);
    }
 
    public String accountImport(String userId, String userName, String faceUrl) {
        Integer random = RandomUtils.nextInt(0, 999999999);
        String httpsUrl = getHttpsUrl(TencentCloudImApiEnum.ACCOUNT_IMPORT.getUrl(), random);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Identifier", userId);
        if (StringUtils.isNotEmpty(userName)) {
            jsonObject.put("Nick", userName);
        }
        if (StringUtils.isNotEmpty(faceUrl)) {
            jsonObject.put("FaceUrl", faceUrl);
        }
        log.info("腾讯云im导入单个账号，请求参数：{}", jsonObject.toString());
        String result = doPost(httpsUrl, jsonObject);
        log.info("腾讯云im导入单个账号，返回结果：{}", result);
        return result;
    }
 
    /**
     * 导入多个账号
     * @param userIds 用户id集合
     */
    public void multiAccountImport(List<String> userIds) {
        Integer random = RandomUtils.nextInt(0, 999999999);
        String httpsUrl = getHttpsUrl(TencentCloudImApiEnum.MULTI_ACCOUNT_IMPORT.getUrl(), random);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Accounts", userIds);
        log.info("腾讯云im导入多个账号，请求参数：{}", jsonObject.toString());
        String result = doPost(httpsUrl, jsonObject);
        log.info("腾讯云im导入单个账户，返回结果：{}", result);
    }
 
    /**
     * 批量删除账号
     * @param userIds 用户id集合
     */
    public void accountDeleteBatch(List<String> userIds) {
        Integer random = RandomUtils.nextInt(0, 999999999);
        String httpsUrl = getHttpsUrl(TencentCloudImApiEnum.ACCOUNT_DELETE.getUrl(), random);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("DeleteItem", getUserIdJsonList(userIds));
        log.info("腾讯云im删除账号，请求参数：{}", jsonObject.toString());
        String result = doPost(httpsUrl, jsonObject);
        log.info("腾讯云im删除账户，返回结果：{}", result);
    }
 
    /**
     * 查询账号是否已经导入im
     * @param userIds 用户id集合
     */
    public String accountCheck(List<String> userIds) {
        Integer random = RandomUtils.nextInt(0, 999999999);
        String httpsUrl = getHttpsUrl(TencentCloudImApiEnum.ACCOUNT_CHECK.getUrl(), random);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("CheckItem", getUserIdJsonList(userIds));
        log.info("腾讯云im查询账号，请求参数：{}", jsonObject.toString());
        String result = doPost(httpsUrl, jsonObject);
        log.info("腾讯云im查询账号，返回结果：{}", result);
        return result;
    }
 
    private List<JSONObject> getUserIdJsonList(List<String> userIds) {
        return userIds.stream().map(v -> {
            JSONObject userIdJson = new JSONObject();
            userIdJson.put("UserID", v);
            return userIdJson;
        }).collect(Collectors.toList());
    }
 
    /**
     * 单发单聊消息
     * @param syncOtherMachine 是否同步消息到发送方（1-同步，2-不同步）
     * @param fromUserId 发送方用户id
     * @param toUserId 接收方用户id
     * @param msgType 消息对象类型
     * @param msgContent 消息内容
     */
    public String sendMsg(Integer syncOtherMachine, String fromUserId, String toUserId, String msgType, String msgContent) {
        Integer random = RandomUtils.nextInt(0, 999999999);
        String httpsUrl = getHttpsUrl(TencentCloudImApiEnum.SEND_MSG.getUrl(), random);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("SyncOtherMachine", syncOtherMachine);
        if (StringUtils.isNotEmpty(fromUserId)) {
            // 发送方不为空表示指定发送用户，为空表示为管理员发送消息
            jsonObject.put("From_Account", fromUserId);
        }
        jsonObject.put("To_Account", toUserId);
        jsonObject.put("MsgRandom", random);
        List<JSONObject> msgBody = getMsgBody(msgType, msgContent);
        jsonObject.put("MsgBody", msgBody);
        log.info("腾讯云im单发单聊消息，请求参数：{}", jsonObject.toString());
        String result = doPost(httpsUrl, jsonObject);
        log.info("腾讯云im单发单聊消息，返回结果：{}", result);
        return result;
    }
 
    /**
     * 批量发单聊消息
     * @param syncOtherMachine 是否同步消息到发送方（1-同步，2-不同步）
     * @param fromUserId 发送方用户id
     * @param toUserIds 接收方用户id集合
     * @param msgType 消息对象类型
     * @param msgContent 消息内容
     */
    public String batchSendMsg(Integer syncOtherMachine, String fromUserId, List<String> toUserIds, String msgType, String msgContent) {
        Integer random = RandomUtils.nextInt(0, 999999999);
        String httpsUrl = getHttpsUrl(TencentCloudImApiEnum.BATCH_SEND_MSG.getUrl(), random);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("SyncOtherMachine", syncOtherMachine);
        if (StringUtils.isNotEmpty(fromUserId)) {
            // 发送方不为空表示指定发送用户，为空表示为管理员发送消息
            jsonObject.put("From_Account", fromUserId);
        }
        jsonObject.put("To_Account", toUserIds);
        jsonObject.put("MsgRandom", random);
        List<JSONObject> msgBody = getMsgBody(msgType, msgContent);
        jsonObject.put("MsgBody", msgBody);
        log.info("腾讯云im批量发单聊消息，请求参数：{}", jsonObject.toString());
        String result = doPost(httpsUrl, jsonObject);
        log.info("腾讯云im批量发单聊消息，返回结果：{}", result);
        return result;
    }
 
 
    /**
     * 拼接发送消息内容
     * @param msgType 消息类型
     * @param msgContent 发送消息内容
     * @return 消息内容
     */
    private List<JSONObject> getMsgBody(String msgType, String msgContent) {
        List<JSONObject> msgBody = new ArrayList<>();
        if (msgType.equals(TencentCloudImConstantEnum.TIM_TEXT_ELEM.getMessage())) {
            // 文本类型
            JSONObject msgBodyJson = new JSONObject();
            msgBodyJson.put("MsgType", msgType);
            JSONObject msgContentObj = new JSONObject();
            msgContentObj.put("Text", msgContent);
            msgBodyJson.put("MsgContent", msgContentObj);
            msgBody.add(msgBodyJson);
        }
        return msgBody;
    }
 
    /**
     * 查询单聊消息
     * @param fromUserId 发送方用户id
     * @param toUserId 接收方用户id
     * @param maxCnt 查询条数
     * @param startTime 起始时间（单位：秒）
     * @param endTime 结束时间（单位：秒）
     * @param lastMsgKey 最后一条消息的 MsgKey
     * @return 单聊消息列表
     */
    public String adminGetRoamMsg(String fromUserId, String toUserId, Integer maxCnt, Long startTime, Long endTime, String lastMsgKey) {
        Integer random = RandomUtils.nextInt(0, 999999999);
        String httpsUrl = getHttpsUrl(TencentCloudImApiEnum.ADMIN_GET_ROAM_MSG.getUrl(), random);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("From_Account", fromUserId);
        jsonObject.put("To_Account", toUserId);
        jsonObject.put("MaxCnt", maxCnt);
        jsonObject.put("MinTime", startTime);
        jsonObject.put("MaxTime", endTime);
        if (StringUtils.isNotEmpty(lastMsgKey)){
            jsonObject.put("LastMsgKey", lastMsgKey);
        }
        log.info("腾讯云im查询单聊消息，请求参数：{}", jsonObject.toString());
        String result = doPost(httpsUrl, jsonObject);
        log.info("腾讯云im查询单聊消息，返回结果：{}", result);
        return result;
    }
 
    /**
     * 撤回单聊消息
     * @param fromUserId 发送方用户id
     * @param toUserId 接收方用户id
     * @param msgKey MsgKey
     */
    public void adminMsgWithDraw(String fromUserId, String toUserId, String msgKey) {
        Integer random = RandomUtils.nextInt(0, 999999999);
        String httpsUrl = getHttpsUrl(TencentCloudImApiEnum.ADMIN_MSG_WITH_DRAW.getUrl(), random);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("From_Account", fromUserId);
        jsonObject.put("To_Account", toUserId);
        jsonObject.put("MsgKey", msgKey);
        log.info("腾讯云im撤回单聊消息，请求参数：{}", jsonObject.toString());
        String result = doPost(httpsUrl, jsonObject);
        log.info("腾讯云im撤回单聊消息，返回结果：{}", result);
    }
 
    /**
     * 设置单聊消息已读
     * @param reportUserId 读取消息的用户
     * @param peerUserId 发送消息的用户
     */
    public void adminSetMsgRead(String reportUserId, String peerUserId) {
        Integer random = RandomUtils.nextInt(0, 999999999);
        String httpsUrl = getHttpsUrl(TencentCloudImApiEnum.ADMIN_SET_MSG_READ.getUrl(), random);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Report_Account", reportUserId);
        jsonObject.put("Peer_Account", peerUserId);
        log.info("腾讯云im设置单聊消息已读，请求参数：{}", jsonObject.toString());
        String result = doPost(httpsUrl, jsonObject);
        log.info("腾讯云im设置单聊消息已读，返回结果：{}", result);
    }

    /**
     * 查询单聊未读消息计数
     * @param fromUserId 发送方用户ID
     * @param toUserId 接收方用户ID
     * @return 未读消息数量
     */
    public Integer getC2CUnreadMsgNum(String fromUserId, String toUserId) {
        Integer random = RandomUtils.nextInt(0, 999999999);
        String httpsUrl = getHttpsUrl(TencentCloudImApiEnum.GET_C2C_UNREAD_MSG_NUM.getUrl(), random);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("From_Account", fromUserId);
        jsonObject.put("To_Account", toUserId);
        log.info("腾讯云im查询未读消息计数，请求参数：{}", jsonObject.toString());
        String result = doPost(httpsUrl, jsonObject);
        log.info("腾讯云im查询未读消息计数，返回结果：{}", result);
        
        // 解析返回结果获取未读计数
        try {
            JSONObject resultJson = new JSONObject(result);
            if (resultJson.has("UnreadMsgNum")) {
                return resultJson.getInt("UnreadMsgNum");
            }
        } catch (Exception e) {
            log.error("解析未读消息计数失败", e);
        }
        return 0;
    }

    /**
     * 导入单聊消息
     * @param fromUserId 发送方用户ID
     * @param toUserId 接收方用户ID
     * @param msgRandom 消息随机值
     * @param msgTime 消息时间戳
     * @param msgBody 消息内容
     */
    public void importMsg(String fromUserId, String toUserId, Integer msgRandom, Long msgTime, List<JSONObject> msgBody) {
        Integer random = RandomUtils.nextInt(0, 999999999);
        String httpsUrl = getHttpsUrl(TencentCloudImApiEnum.IMPORT_MSG.getUrl(), random);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("From_Account", fromUserId);
        jsonObject.put("To_Account", toUserId);
        jsonObject.put("MsgRandom", msgRandom);
        jsonObject.put("MsgTime", msgTime);
        jsonObject.put("MsgBody", msgBody);
        log.info("腾讯云im导入单聊消息，请求参数：{}", jsonObject.toString());
        String result = doPost(httpsUrl, jsonObject);
        log.info("腾讯云im导入单聊消息，返回结果：{}", result);
    }

    /**
     * 拉取会话列表
     * @param userId 用户ID
     * @param limit 拉取数量
     * @return 会话列表
     */
    public String getContactList(String userId, Integer limit) {
        Integer random = RandomUtils.nextInt(0, 999999999);
        String httpsUrl = getHttpsUrl(TencentCloudImApiEnum.CONTACT_GET_LIST.getUrl(), random);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("From_Account", userId);
        jsonObject.put("Count", limit);
        log.info("腾讯云im拉取会话列表，请求参数：{}", jsonObject.toString());
        String result = doPost(httpsUrl, jsonObject);
        log.info("腾讯云im拉取会话列表，返回结果：{}", result);
        return result;
    }

    /**
     * 删除单个会话
     * @param userId 用户ID
     * @param peerUserId 对方用户ID
     */
    public void deleteContact(String userId, String peerUserId) {
        Integer random = RandomUtils.nextInt(0, 999999999);
        String httpsUrl = getHttpsUrl(TencentCloudImApiEnum.CONTACT_DELETE.getUrl(), random);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("From_Account", userId);
        jsonObject.put("To_Account", peerUserId);
        log.info("腾讯云im删除单个会话，请求参数：{}", jsonObject.toString());
        String result = doPost(httpsUrl, jsonObject);
        log.info("腾讯云im删除单个会话，返回结果：{}", result);
    }

    /**
     * 发送同步消息（用于MessageServiceImpl）
     * @param syncOtherMachine 同步到发送方
     * @param fromUserId 发送方
     * @param toUserId 接收方
     * @param msgType 消息类型
     * @param msgContent 消息内容
     * @return TencentImResult
     */
    public TencentImResult sendMsgSync(Integer syncOtherMachine, String fromUserId, String toUserId, String msgType, String msgContent) {
        String result = sendMsg(syncOtherMachine, fromUserId, toUserId, msgType, msgContent);
        TencentImResult tencentImResult = new TencentImResult();
        tencentImResult.setRawResult(result);
        
        try {
            JSONObject resultJson = new JSONObject(result);
            tencentImResult.setErrorCode(resultJson.optInt("ErrorCode"));
            tencentImResult.setErrorMessage(resultJson.optString("ErrorInfo"));
            tencentImResult.setMsgRandom(resultJson.optInt("MsgRandom"));
            tencentImResult.setMsgTime(resultJson.optLong("MsgTime"));
            tencentImResult.setMsgKey(resultJson.optString("MsgKey"));
            tencentImResult.setFromAccount(fromUserId);
            tencentImResult.setToAccount(toUserId);
            tencentImResult.setSuccess(resultJson.optInt("ErrorCode") == 0);
        } catch (Exception e) {
            log.error("解析腾讯IM返回结果失败", e);
            tencentImResult.setErrorCode(-1);
            tencentImResult.setErrorMessage("解析失败: " + e.getMessage());
            tencentImResult.setSuccess(false);
        }
        
        return tencentImResult;
    }

    private String doPost(String url, JSONObject jsonObject) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");

            StringEntity entity = new StringEntity(jsonObject.toString(), "UTF-8");
            httpPost.setEntity(entity);

            return httpClient.execute(httpPost, response -> {
                return EntityUtils.toString(response.getEntity(), "UTF-8");
            });
        } catch (IOException e) {
            log.error("HTTP请求失败", e);
            throw new RuntimeException("HTTP请求失败", e);
        }
    }
}
