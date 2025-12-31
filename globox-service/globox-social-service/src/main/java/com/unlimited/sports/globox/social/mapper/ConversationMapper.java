package com.unlimited.sports.globox.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.social.entity.Conversation;
import com.unlimited.sports.globox.model.social.entity.MessageTypeEnum;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会话Mapper接口
 */
public interface ConversationMapper extends BaseMapper<Conversation> {

    /**
     * 根据用户ID查询会话列表
     */
    List<Conversation> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询两个用户之间的会话
     */
    Conversation selectByUserPair(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * 更新会话置顶状态
     */
    int updatePinned(@Param("conversationId") Long conversationId, @Param("userId") Long userId, @Param("isPinned") Boolean isPinned);

    /**
     * 更新会话屏蔽状态
     */
    int updateBlocked(@Param("conversationId") Long conversationId, @Param("userId") Long userId, @Param("isBlocked") Boolean isBlocked);

    /**
     * 清除未读计数
     */
    int clearUnreadCount(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    /**
     * 标记会话已读
     */
    int markConversationRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    /**
     * 逻辑删除会话
     */
    int logicalDelete(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    /**
     * 更新最后消息信息
     */
    int updateLastMessage(@Param("conversationId") Long conversationId, 
                         @Param("messageId") Long messageId, 
                         @Param("content") String content, 
                         @Param("messageType") MessageTypeEnum messageType);

    /**
     * 更新未读计数（发送方+1）
     */
    int incrementSenderUnread(@Param("conversationId") Long conversationId);

    /**
     * 更新未读计数（接收方+1）
     */
    int incrementReceiverUnread(@Param("conversationId") Long conversationId);

    /**
     * 更新发送方的未读计数
     * @param conversationId 会话ID
     * @param unreadCount 未读数量
     */
    int updateSenderUnreadCount(@Param("conversationId") Long conversationId, @Param("unreadCount") Integer unreadCount);

    /**
     * 更新接收方的未读计数
     * @param conversationId 会话ID
     * @param unreadCount 未读数量
     */
    int updateReceiverUnreadCount(@Param("conversationId") Long conversationId, @Param("unreadCount") Integer unreadCount);

    /**
     * 插入会话
     */
    int insertConversation(Conversation conversation);

    /**
     * 查询会话详情
     */
    Conversation selectByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 根据用户ID查询会话列表（支持数据库分页）
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 会话列表
     */
    List<Conversation> selectByUserIdWithPagination(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 根据用户ID查询会话总数
     * @param userId 用户ID
     * @return 会话总数
     */
    Long countByUserId(@Param("userId") Long userId);

    /**
     * 重置指定用户的未读计数
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 影响行数
     */
    int resetUnreadCount(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
