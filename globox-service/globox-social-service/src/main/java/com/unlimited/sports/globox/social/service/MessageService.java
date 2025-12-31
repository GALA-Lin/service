package com.unlimited.sports.globox.social.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.model.social.dto.MessageDto;
import com.unlimited.sports.globox.model.social.entity.MessageEntity;

import java.util.List;

/**
 * 消息服务接口
 */
public interface MessageService extends IService<MessageEntity> {

    /**
     * 发送消息
     * @param messageDto 消息传输对象
     * @param conversationId 会话ID
     * @return 发送结果
     */
    String sendMessage(MessageDto messageDto, Long conversationId);

    /**
     * 批量发送消息
     * @param fromUserId 发送方用户ID
     * @param toUserIds 接收方用户ID列表
     * @param messageDto 消息传输对象
     * @return 发送结果
     */
    String batchSendMessage(Long fromUserId, List<Long> toUserIds, MessageDto messageDto);

    /**
     * 撤回消息
     * @param messageId 消息ID
     * @param userId 操作用户ID
     * @return 撤回结果
     */
    String recallMessage(Long messageId, Long userId);

    /**
     * 查询单聊消息
     * @param fromUserId 发送方用户ID
     * @param toUserId 接收方用户ID
     * @param maxCnt 查询条数
     * @param startTime 起始时间（单位：秒）
     * @param endTime 结束时间（单位：秒）
     * @return 消息列表
     */
    List<MessageEntity> queryChatMessages(Long fromUserId, Long toUserId, Integer maxCnt, Long startTime, Long endTime);

    /**
     * 查询单聊未读消息计数
     * @param fromUserId 发送方用户ID
     * @param toUserId 接收方用户ID
     * @return 未读消息数量
     */
    Integer getC2CUnreadMsgNum(Long fromUserId, Long toUserId);

    /**
     * 删除消息（逻辑删除）
     * @param messageId 消息ID
     * @param userId 操作用户ID
     * @return 删除结果
     */
    String deleteMessage(Long messageId, Long userId);

    /**
     * 导入消息到数据库
     * @param messageEntity 消息实体
     * @return 导入结果
     */
    String importMessageToDB(MessageEntity messageEntity);

    /**
     * 从Redis同步消息到数据库
     * @param fromUserId 发送方用户ID
     * @param toUserId 接收方用户ID
     * @return 同步数量
     */
    Integer syncMessageFromRedis(Long fromUserId, Long toUserId);

    /**
     * 根据消息ID查询消息详情
     * @param messageId 消息ID
     * @return 消息实体
     */
    MessageEntity getMessageDetail(Long messageId);

    /**
     * 根据会话ID查询消息列表
     * @param conversationId 会话ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 消息列表
     */
    PaginationResult<MessageEntity> getMessageListByConversation(Long conversationId, Integer page, Integer pageSize);

}
