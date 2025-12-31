package com.unlimited.sports.globox.social.service;

import org.json.JSONObject;

import java.util.List;

/**
 * 腾讯云IM服务接口
 * 提供腾讯云IM相关功能的业务逻辑处理
 */
public interface TencentCloudImService {

    /**
     * 获取腾讯云用户签名
     * @param userId 用户ID
     * @return 用户签名
     */
    String getTxCloudUserSig(String userId);

    /**
     * 导入单个账号
     * @param userId 用户ID
     * @param userName 用户名
     * @param faceUrl 头像URL
     */
    void accountImport(String userId, String userName, String faceUrl);

    /**
     * 导入单个账号（仅用户ID）
     * @param userId 用户ID
     */
    void accountImport(String userId);

    /**
     * 导入多个账号
     * @param userIds 用户ID列表
     */
    void multiAccountImport(List<String> userIds);

    /**
     * 批量删除账号
     * @param userIds 用户ID列表
     */
    void accountDeleteBatch(List<String> userIds);

    /**
     * 查询账号是否已经导入IM
     * @param userIds 用户ID列表
     * @return 查询结果
     */
    String accountCheck(List<String> userIds);

    /**
     * 单发单聊消息
     * @param syncOtherMachine 是否同步消息到发送方(1-同步，2-不同步)
     * @param fromUserId 发送方用户ID
     * @param toUserId 接收方用户ID
     * @param msgType 消息类型
     * @param msgContent 消息内容
     * @return 发送结果
     */
    String sendMsg(Integer syncOtherMachine, String fromUserId, String toUserId, String msgType, String msgContent);

    /**
     * 批量发单聊消息
     * @param syncOtherMachine 是否同步消息到发送方(1-同步，2-不同步)
     * @param fromUserId 发送方用户ID
     * @param toUserIds 接收方用户ID列表
     * @param msgType 消息类型
     * @param msgContent 消息内容
     * @return 发送结果
     */
    String batchSendMsg(Integer syncOtherMachine, String fromUserId, List<String> toUserIds, String msgType, String msgContent);

    /**
     * 查询单聊消息
     * @param fromUserId 发送方用户ID
     * @param toUserId 接收方用户ID
     * @param maxCnt 查询条数
     * @param startTime 起始时间（单位：秒）
     * @param endTime 结束时间（单位：秒）
     * @param lastMsgKey 最后一条消息的MsgKey
     * @return 消息列表
     */
    String adminGetRoamMsg(String fromUserId, String toUserId, Integer maxCnt, Long startTime, Long endTime, String lastMsgKey);

    /**
     * 撤回单聊消息
     * @param fromUserId 发送方用户ID
     * @param toUserId 接收方用户ID
     * @param msgKey 消息Key
     */
    void adminMsgWithDraw(String fromUserId, String toUserId, String msgKey);

    /**
     * 设置单聊消息已读
     * @param reportUserId 读取消息的用户
     * @param peerUserId 发送消息的用户
     */
    void adminSetMsgRead(String reportUserId, String peerUserId);

    /**
     * 查询单聊未读消息计数
     * @param fromUserId 发送方用户ID
     * @param toUserId 接收方用户ID
     * @return 未读消息数量
     */
    Integer getC2CUnreadMsgNum(String fromUserId, String toUserId);

    /**
     * 导入单聊消息
     * @param fromUserId 发送方用户ID
     * @param toUserId 接收方用户ID
     * @param msgRandom 消息随机值
     * @param msgTime 消息时间戳
     * @param msgBody 消息内容
     */
    void importMsg(String fromUserId, String toUserId, Integer msgRandom, Long msgTime, List<JSONObject> msgBody);

    /**
     * 拉取会话列表
     * @param userId 用户ID
     * @param limit 拉取数量
     * @return 会话列表
     */
    String getContactList(String userId, Integer limit);

    /**
     * 删除单个会话
     * @param userId 用户ID
     * @param peerUserId 对方用户ID
     */
    void deleteContact(String userId, String peerUserId);
}
