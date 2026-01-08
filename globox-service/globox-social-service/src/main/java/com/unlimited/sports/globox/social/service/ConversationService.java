package com.unlimited.sports.globox.social.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.model.social.vo.ConversationVo;
import com.unlimited.sports.globox.model.social.entity.Conversation;
import com.unlimited.sports.globox.model.social.entity.MessageTypeEnum;

/**
 * 会话服务接口
 */
public interface ConversationService extends IService<Conversation> {

    /**
     * 获取用户会话列表
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页结果对象（会话Vo）
     */
    PaginationResult<ConversationVo> getConversationVoList(Long userId, Integer page, Integer pageSize);

    /**
     * 获取用户会话列表
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页结果对象(会话)
     */
    PaginationResult<Conversation> getConversationList(Long userId, Integer page, Integer pageSize);


    /**
     * 获取或创建会话 - 如果会话存在则返回，不存在则创建
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 会话实体
     */
    Conversation getOrCreateConversation(Long userId, Long friendId);

    /**
     * 根据会话ID获取会话详情
     * @param conversationId 会话ID
     * @return 会话实体
     */
    Conversation getConversationById(Long conversationId);

    /**
     * 置顶/取消置顶会话
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param isPinned 是否置顶
     * @return 是否成功
     */
    Boolean togglePinned(Long conversationId, Long userId, Boolean isPinned);

    /**
     * 屏蔽/取消屏蔽会话
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param isBlocked 是否屏蔽
     * @return 是否成功
     */
    Boolean toggleBlocked(Long conversationId, Long userId, Boolean isBlocked);

    /**
     * 清除用户所有会话的未读计数
     * @param userId 用户ID
     * @return 是否成功
     */
    Boolean clearUnreadCount(Long userId);


    /**
     * 标记会话已读
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 是否成功
     */
    Boolean markConversationRead(Long conversationId, Long userId);

    /**
     * 删除会话
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 是否成功
     */
    Boolean deleteConversation(Long conversationId, Long userId);

    /**
     * 删除会话（通过用户对）
     * @param userId 用户ID
     * @param peerUserId 对方用户ID
     * @return 是否成功
     */
    Boolean deleteContact(Long userId, String peerUserId);

    /**
     * 更新会话最后消息
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @param content 消息内容
     * @param messageType 消息类型
     * @return 是否成功
     */
    Boolean updateLastMessage(Long conversationId, Long messageId, String content, MessageTypeEnum messageType);

    /**
     * 增加未读计数
     * @param conversationId 会话ID
     * @param fromUserId 发送方ID
     * @param toUserId 接收方ID
     * @return 是否成功
     */
    Boolean incrementUnreadCount(Long conversationId, Long fromUserId, Long toUserId);

    /**
     * 重置会话未读计数（设置指定用户的未读数为0）
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 是否成功
     */
    Boolean resetUnreadCount(Long conversationId, Long userId);

}
