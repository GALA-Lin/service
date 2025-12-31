package com.unlimited.sports.globox.social.service.impl;

import com.unlimited.sports.globox.social.service.TencentCloudImService;
import com.unlimited.sports.globox.social.util.TencentCloudImUtil;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 腾讯云IM服务实现类

 */
@Slf4j
@Service
public class TencentCloudImServiceImpl implements TencentCloudImService {

    @Autowired
    private TencentCloudImUtil tencentCloudImUtil;

    @Override
    public String getTxCloudUserSig(String userId) {
        return tencentCloudImUtil.getTxCloudUserSig(userId);
    }

    @Override
    public void accountImport(String userId, String userName, String faceUrl) {
        tencentCloudImUtil.accountImport(userId, userName, faceUrl);
    }

    @Override
    public void accountImport(String userId) {
        tencentCloudImUtil.accountImport(userId);
    }

    @Override
    public void multiAccountImport(List<String> userIds) {
        tencentCloudImUtil.multiAccountImport(userIds);
    }

    @Override
    public void accountDeleteBatch(List<String> userIds) {
        tencentCloudImUtil.accountDeleteBatch(userIds);
    }

    @Override
    public String accountCheck(List<String> userIds) {
        return tencentCloudImUtil.accountCheck(userIds);
    }

    @Override
    public String sendMsg(Integer syncOtherMachine, String fromUserId, String toUserId, String msgType, String msgContent) {
        return tencentCloudImUtil.sendMsg(syncOtherMachine, fromUserId, toUserId, msgType, msgContent);
    }

    @Override
    public String batchSendMsg(Integer syncOtherMachine, String fromUserId, List<String> toUserIds, String msgType, String msgContent) {
        return tencentCloudImUtil.batchSendMsg(syncOtherMachine, fromUserId, toUserIds, msgType, msgContent);
    }

    @Override
    public String adminGetRoamMsg(String fromUserId, String toUserId, Integer maxCnt, Long startTime, Long endTime, String lastMsgKey) {
        return tencentCloudImUtil.adminGetRoamMsg(fromUserId, toUserId, maxCnt, startTime, endTime, lastMsgKey);
    }

    @Override
    public void adminMsgWithDraw(String fromUserId, String toUserId, String msgKey) {
        tencentCloudImUtil.adminMsgWithDraw(fromUserId, toUserId, msgKey);
    }

    @Override
    public void adminSetMsgRead(String reportUserId, String peerUserId) {
        tencentCloudImUtil.adminSetMsgRead(reportUserId, peerUserId);
    }

    @Override
    public Integer getC2CUnreadMsgNum(String fromUserId, String toUserId) {
        return tencentCloudImUtil.getC2CUnreadMsgNum(fromUserId, toUserId);
    }

    @Override
    public void importMsg(String fromUserId, String toUserId, Integer msgRandom, Long msgTime, List<JSONObject> msgBody) {
        tencentCloudImUtil.importMsg(fromUserId, toUserId, msgRandom, msgTime, msgBody);
    }

    @Override
    public String getContactList(String userId, Integer limit) {
        return tencentCloudImUtil.getContactList(userId, limit);
    }

    @Override
    public void deleteContact(String userId, String peerUserId) {
        tencentCloudImUtil.deleteContact(userId, peerUserId);
    }
}
