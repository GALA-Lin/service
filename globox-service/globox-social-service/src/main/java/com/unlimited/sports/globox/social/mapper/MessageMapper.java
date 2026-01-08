package com.unlimited.sports.globox.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.social.entity.MessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息Mapper接口
 */
@Mapper
public interface MessageMapper extends BaseMapper<MessageEntity> {

    /**
     * 根据随机值查询消息
     * @param fromUserId 发送方用户ID
     * @param toUserId 接收方用户ID
     * @param random 消息随机值
     * @return 消息实体
     */
    MessageEntity selectByRandom(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId, @Param("random") Long random);

    /**
     * 根据消息ID查询消息详情（不包含已删除）
     * @param messageId 消息ID
     * @return 消息实体
     */
    MessageEntity selectByIdNotDeleted(@Param("messageId") Long messageId);

    /**
     * 根据会话ID查询消息列表
     * @param conversationId 会话ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 消息列表
     */
    List<MessageEntity> selectByConversationId(@Param("conversationId") Long conversationId, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * 查询两个用户之间的消息
     * @param fromUserId 发送方用户ID
     * @param toUserId 接收方用户ID
     * @param maxCnt 最大数量
     * @param startTime 开始时间戳(可选)
     * @param endTime 结束时间戳(可选)
     * @return 消息列表
     */
    List<MessageEntity> selectBetweenUsers(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId,
                                           @Param("maxCnt") Integer maxCnt, @Param("startTime") Long startTime, @Param("endTime") Long endTime);

    /**
     * 查询两个用户之间的未读消息（接收方未读的消息）
     * @param fromUserId 发送方用户ID
     * @param toUserId 接收方用户ID
     * @return 未读消息列表
     */
    List<MessageEntity> selectUnreadMessages(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);

    /**
     * 查询两个用户之间的未读消息数量
     * @param fromUserId 发送方用户ID
     * @param toUserId 接收方用户ID
     * @return 未读消息数量
     */
    Integer selectUnreadCount(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);

    /**
     * 根据会话ID查询消息总条数
     * @param conversationId 会话ID
     * @return 消息总条数
     */
    Long selectCountByConversationId(@Param("conversationId") Long conversationId);
}
