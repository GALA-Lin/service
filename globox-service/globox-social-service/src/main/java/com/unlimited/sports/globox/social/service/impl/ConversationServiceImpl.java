package com.unlimited.sports.globox.social.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.model.social.entity.*;
import com.unlimited.sports.globox.model.social.vo.ConversationVo;
import com.unlimited.sports.globox.social.mapper.ConversationMapper;
import com.unlimited.sports.globox.social.mapper.MessageMapper;
import com.unlimited.sports.globox.social.service.ConversationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * 会话服务实现类
 */
@Slf4j
@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements ConversationService {

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private  MessageMapper messageMapper;

    @DubboReference(group="rpc")
    UserDubboService userDubboService;



    @Override
    public PaginationResult<ConversationVo> getConversationVoList(Long userId, Integer page, Integer pageSize) {
        try {
            // 计算分页偏移量
            int offset = (page - 1) * pageSize;
            List<Conversation> pagedConversations = conversationMapper.selectByUserIdWithPagination(userId, offset, pageSize);

            List<ConversationVo> conversationVoList = pagedConversations.stream()
                    .filter(conversation -> {
                        // 检查用户是否是会话的参与者
                        return conversation.getSenderUserId().equals(userId) ||
                                conversation.getReceiveUserId().equals(userId);
                    })
                    .map(conversation -> {
                        ConversationVo vo = new ConversationVo();
                        vo.setConversationId(String.valueOf(conversation.getConversationId()));
                        vo.setConversationType(conversation.getConversationType());
                        vo.setReceiveUserId(choice(userId,conversation));
                        vo.setConversationNameReceiver(getUserInfoVo(userId, conversation).getNickName());
                        vo.setConversationAvatarReceiver(getUserInfoVo(userId, conversation).getAvatarUrl());
                        vo.setUnreadCountSender(conversation.getUnreadCountSender());
                        vo.setUnreadCountReceiver(conversation.getUnreadCountReceiver());
                        vo.setIsBlocked(conversation.getIsBlocked());
                        vo.setIsPinnedSender(conversation.getIsPinnedSender());
                        vo.setIsPinnedReceiver(conversation.getIsPinnedReceiver());
                        vo.setIsDeletedSender(conversation.getIsDeletedSender());
                        vo.setIsDeletedReceiver(conversation.getIsDeletedReceiver());
                        vo.setLastMessageId(conversation.getLastMessageId());
                        vo.setLastMessageContent(conversation.getLastMessageContent());
                        vo.setLastMessageType(conversation.getLastMessageType());
                        vo.setLastMessageAt(conversation.getLastMessageAt());
                        vo.setCreatedAt(conversation.getCreatedAt());
                        vo.setUpdatedAt(conversation.getUpdatedAt());
                        return vo;
                    })
                    .collect(Collectors.toList());
            // 查询总数
            Long total = conversationMapper.countByUserId(userId);
            return PaginationResult.build(
                    pagedConversations != null ? conversationVoList : emptyList(),
                    total != null ? total : 0L,
                    page != null ? page : 1,
                    pageSize != null ? pageSize : 50
            );
        } catch (Exception e) {
            log.error("查询会话列表失败", e);
            throw new RuntimeException("查询会话列表失败: " + e.getMessage());
        }
    }

    @Override
    public PaginationResult<Conversation> getConversationList(Long userId, Integer page, Integer pageSize) {
        try {
            // 计算分页偏移量
            int offset = (page - 1) * pageSize;
            List<Conversation> pagedConversations = conversationMapper.selectByUserIdWithPagination(userId, offset, pageSize);
            // 查询总数
            Long total = conversationMapper.countByUserId(userId);
            return PaginationResult.build(
                    pagedConversations != null ? pagedConversations : emptyList(),
                    total != null ? total : 0L,
                    page != null ? page : 1,
                    pageSize != null ? pageSize : 50
            );
        } catch (Exception e) {
            log.error("查询会话列表失败", e);
            throw new RuntimeException("查询会话列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Conversation getOrCreateConversation(Long userId, Long friendId) {
        try {
            if (userId.equals(friendId)){
                log.error("用户不能与自己进行会话");
                return null;
            }
            // 先检查会话是否已存在
            Conversation existing = conversationMapper.selectByUserPair(userId, friendId);
            if (existing != null) {

                log.info("会话已存在，返回现有会话: {}", existing.getConversationId());
                return existing;
            }

            // 会话不存在，创建新会话
            log.info("会话不存在，开始创建新会话: userId={}, friendId={}", userId, friendId);


            RpcResult<UserInfoVo> rpcResult = userDubboService.getUserInfo(friendId);
            Assert.rpcResultOk(rpcResult);
            UserInfoVo friendInfo = rpcResult.getData();
            log.info("获取用户信息成功: {}", friendInfo);

            RpcResult<UserInfoVo> rpcResult2 = userDubboService.getUserInfo(userId);
            Assert.rpcResultOk(rpcResult2);
            UserInfoVo userInfo = rpcResult2.getData();
            log.info("获取用户信息成功: {}", userInfo);

            // 使用Builder模式创建新会话
            Conversation conversation = Conversation.builder()
                    .conversationType(ConversationTypeEnum.PRIVATE)
                    .senderUserId(Math.min(userId, friendId))
                    .receiveUserId(Math.max(userId, friendId))
                    .conversationNameSender(friendInfo.getNickName())
                    .conversationAvatarSender(friendInfo.getAvatarUrl())
                    .conversationNameReceiver(userInfo.getNickName())
                    .conversationAvatarReceiver(userInfo.getAvatarUrl())
                    .unreadCountSender(0L)
                    .unreadCountReceiver(0L)
                    .isBlocked(false)
                    .isPinnedSender(false)
                    .isPinnedReceiver(false)
                    .isDeletedSender(false)
                    .isDeletedReceiver(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            conversationMapper.insert(conversation);
            log.info("创建会话成功: {}", conversation.getConversationId());
            return conversation;

        } catch (Exception e) {
            log.error("获取或创建会话失败", e);
            throw new RuntimeException("获取或创建会话失败: " + e.getMessage());
        }
    }

    @Override
    public Conversation getConversationById(Long conversationId) {
        try {
            return conversationMapper.selectByConversationId(conversationId);
        } catch (Exception e) {
            log.error("查询会话详情失败", e);
            throw new RuntimeException("查询会话详情失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean togglePinned(Long conversationId, Long userId, Boolean isPinned) {
        try {
            // 先查询会话详情
            Conversation conversation = conversationMapper.selectByConversationId(conversationId);
            if (conversation == null) {
                log.warn("置顶操作失败：会话不存在, conversationId={}, userId={}", conversationId, userId);
                return false;
            }

            // 验证用户是否是会话成员
            if (!conversation.getSenderUserId().equals(userId) && !conversation.getReceiveUserId().equals(userId)) {
                log.warn("置顶操作失败：用户{}不是会话{}的成员, userId={}", userId, conversationId, userId);
                return false;
            }

            // 验证用户是否已被删除（逻辑删除）
            if (conversation.getSenderUserId().equals(userId) && conversation.getIsDeletedSender()) {
                log.warn("置顶操作失败：用户{}已删除会话{}", userId, conversationId);
                return false;
            }
            if (conversation.getReceiveUserId().equals(userId) && conversation.getIsDeletedReceiver()) {
                log.warn("置顶操作失败：用户{}已删除会话{}", userId, conversationId);
                return false;
            }

            int result = conversationMapper.updatePinned(conversationId, userId, isPinned);
            if (result > 0) {
                if (isPinned) {
                    log.info("用户{}成功置顶会话{}", userId, conversationId);
                    return true;  // 置顶返回true
                } else {
                    log.info("用户{}成功取消置顶会话{}", userId, conversationId);
                    return false;  // 取消置顶返回false
                }
            } else {
                log.warn("置顶操作失败，可能未发生变更: conversationId={}, userId={}", conversationId, userId);
                return false;
            }
        } catch (Exception e) {
            log.error("置顶会话失败, conversationId={}, userId={}", conversationId, userId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean toggleBlocked(Long conversationId, Long userId, Boolean isBlocked) {
        try {
            // 先查询会话详情
            Conversation conversation = conversationMapper.selectById(conversationId);
            if (conversation == null) {
                log.warn("屏蔽操作失败：会话不存在, conversationId={}, userId={}", conversationId, userId);
                return false;
            }

            // 验证用户是否是会话成员
            if (!conversation.getSenderUserId().equals(userId) && !conversation.getReceiveUserId().equals(userId)) {
                log.warn("屏蔽操作失败：用户{}不是会话{}的成员, userId={}", userId, conversationId, userId);
                return false;
            }

            // 验证用户是否已被删除（逻辑删除）
            if (conversation.getSenderUserId().equals(userId) && conversation.getIsDeletedSender()) {
                log.warn("屏蔽操作失败：用户{}已删除会话{}", userId, conversationId);
                return false;
            }
            if (conversation.getReceiveUserId().equals(userId) && conversation.getIsDeletedReceiver()) {
                log.warn("屏蔽操作失败：用户{}已删除会话{}", userId, conversationId);
                return false;
            }

            int result = conversationMapper.updateBlocked(conversationId, userId, isBlocked);
            if (result > 0) {
                if (isBlocked) {
                    log.info("用户{}成功屏蔽会话{}", userId, conversationId);
                    return true;  // 屏蔽返回true
                } else {
                    log.info("用户{}成功取消屏蔽会话{}", userId, conversationId);
                    return false;  // 取消屏蔽返回false
                }
            } else {
                log.warn("屏蔽操作失败，可能未发生变更: conversationId={}, userId={}", conversationId, userId);
                return false;
            }
        } catch (Exception e) {
            log.error("屏蔽会话失败, conversationId={}, userId={}", conversationId, userId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean clearUnreadCount(Long userId) {
        try {
            log.info("用户{}开始清除所有会话的未读计数", userId);

            // 1. 获取用户的所有会话列表
            List<Conversation> conversations = conversationMapper.selectByUserId(userId);

            if (conversations == null || conversations.isEmpty()) {
                log.info("用户{}没有会话，无需清除未读计数", userId);
                return true;
            }

            long totalCleared = conversations.stream()
                    .mapToLong(conversation -> {
                        try {
                            // 查询该会话下用户作为接收方的未读消息
                            List<MessageEntity> unreadMessageEntities;

                            if (conversation.getSenderUserId().equals(userId)) {
                                // 当前用户是发送方，查询发送方是自己、接收方是对方的未读消息
                                unreadMessageEntities = messageMapper.selectUnreadMessages(
                                        userId,
                                        conversation.getReceiveUserId()
                                );
                            } else {
                                // 当前用户是接收方，查询发送方是对方、接收方是自己的未读消息
                                unreadMessageEntities = messageMapper.selectUnreadMessages(
                                        conversation.getSenderUserId(),
                                        userId
                                );
                            }

                            // 将所有未读消息标记为已读
                            long messagesMarked = 0;
                            if (unreadMessageEntities != null && !unreadMessageEntities.isEmpty()) {
                                messagesMarked = unreadMessageEntities.stream()
                                        .filter(message -> !message.getIsRead())
                                        .peek(message -> {
                                            message.setIsRead(true);
                                            message.setStatus(MessageStatusEnum.READ);
                                            message.setReadTime(LocalDateTime.now());
                                            message.setUpdatedAt(LocalDateTime.now());
                                            messageMapper.updateById(message);
                                        })
                                        .count();

                                log.info("将会话{}下的{}条未读消息标记为已读",
                                        conversation.getConversationId(), unreadMessageEntities.size());
                            } else {
                                log.info("会话{}下没有未读消息", conversation.getConversationId());
                            }

                            // 清除该会话的未读计数
                            int cleared = conversationMapper.clearUnreadCount(conversation.getConversationId(), userId);
                            if (cleared > 0) {
                                log.info("清除会话{}的未读计数成功", conversation.getConversationId());
                                return 1L; // 统计清除计数的会话数
                            } else {
                                log.info("会话{}的未读计数已经是0", conversation.getConversationId());
                                return 0L;
                            }
                        } catch (Exception e) {
                            log.error("清除会话{}的未读计数失败", conversation.getConversationId(), e);
                            return 0L; // 继续处理其他会话
                        }
                    })
                    .sum();

            // 统计总的消息标记数
            long totalMessagesMarked = conversations.stream()
                    .mapToLong(conversation -> {
                        try {
                            List<MessageEntity> unreadMessageEntities;
                            if (conversation.getSenderUserId().equals(userId)) {
                                unreadMessageEntities = messageMapper.selectUnreadMessages(
                                        userId,
                                        conversation.getReceiveUserId()
                                );
                            } else {
                                unreadMessageEntities = messageMapper.selectUnreadMessages(
                                        conversation.getSenderUserId(),
                                        userId
                                );
                            }

                            if (unreadMessageEntities != null && !unreadMessageEntities.isEmpty()) {
                                return unreadMessageEntities.stream()
                                        .filter(message -> !message.getIsRead())
                                        .count();
                            }
                            return 0L;
                        } catch (Exception e) {
                            return 0L;
                        }
                    })
                    .sum();

            log.info("用户{}清除所有会话未读计数完成，共清除{}个会话的计数，标记{}条消息为已读",
                    userId, totalCleared, totalMessagesMarked);

            return true;
        } catch (Exception e) {
            log.error("清除所有未读计数失败", e);
            throw new RuntimeException("清除所有未读计数失败: " + e.getMessage());
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean markConversationRead(Long conversationId, Long userId) {
        try {
            // 1. 获取会话详情，确定用户角色
            Conversation conversation = conversationMapper.selectById(conversationId);
            if (conversation == null) {
                log.info("会话不存在: {}", conversationId);
                return false;
            }

            // 2. 标记会话已读（清除未读计数）
            int result = conversationMapper.markConversationRead(conversationId, userId);

            // 3. 查询该会话下用户作为接收方的未读消息
            List<MessageEntity> unreadMessageEntities;

            if (conversation.getSenderUserId().equals(userId)) {
                // 当前用户是发送方，不需要标记消息已读（发送方的消息本来就是已读的）
                log.info("用户{}是会话{}的发送方，无需标记消息已读", userId, conversationId);
                return result > 0;
            } else {
                // 当前用户是接收方，查询发送方是对方、接收方是当前用户的未读消息
                unreadMessageEntities = messageMapper.selectUnreadMessages(
                        conversation.getSenderUserId(),    // 发送方是对方
                        conversation.getReceiveUserId()    // 接收方是当前用户
                );
            }

            // 4. 将所有未读消息标记为已读
            if (unreadMessageEntities != null && !unreadMessageEntities.isEmpty()) {
                for (MessageEntity messageEntity : unreadMessageEntities) {
                    if (!messageEntity.getIsRead() && messageEntity.getToUserId().equals(userId)) {
                        // 直接通过Mapper更新消息状态
                        messageEntity.setIsRead(true);
                        messageEntity.setStatus(MessageStatusEnum.READ);
                        messageEntity.setReadTime(LocalDateTime.now());
                        messageEntity.setUpdatedAt(LocalDateTime.now());
                        messageMapper.updateById(messageEntity);
                    }
                }
                log.info("会话{}下的{}条未读消息已标记为已读", conversationId, unreadMessageEntities.size());
            } else {
                log.info("会话{}下没有未读消息", conversationId);
            }

            return result > 0;
        } catch (Exception e) {
            log.error("标记会话已读失败", e);
            throw new RuntimeException("标记会话已读失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteConversation(Long conversationId, Long userId) {
        try {
            // 先查询会话详情
            Conversation conversation = conversationMapper.selectById(conversationId);
            if (conversation == null) {
                log.warn("删除操作失败：会话不存在, conversationId={}, userId={}", conversationId, userId);
                return false;
            }

            // 验证用户是否是会话成员
            if (!conversation.getSenderUserId().equals(userId) && !conversation.getReceiveUserId().equals(userId)) {
                log.warn("删除操作失败：用户{}不是会话{}的成员, userId={}", userId, conversationId, userId);
                return false;
            }

            int result = conversationMapper.logicalDelete(conversationId, userId);
            if (result > 0) {
                log.info("用户{}成功删除会话{}", userId, conversationId);
                return true;
            } else {
                log.warn("删除会话失败，可能未发生变更: conversationId={}, userId={}", conversationId, userId);
                return false;
            }
        } catch (Exception e) {
            log.error("删除会话失败, conversationId={}, userId={}", conversationId, userId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteContact(Long userId, String peerUserId) {
        try {
            Long peerId = Long.parseLong(peerUserId);
            Conversation conversation = conversationMapper.selectByUserPair(userId, peerId);

            if (conversation == null) {
                return true; // 会话不存在，视为成功
            }

            int result = conversationMapper.logicalDelete(conversation.getConversationId(), userId);
            return result > 0;
        } catch (Exception e) {
            log.error("删除会话失败", e);
            throw new RuntimeException("删除会话失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateLastMessage(Long conversationId, Long messageId, String content, MessageTypeEnum messageType) {
        try {
            int result = conversationMapper.updateLastMessage(conversationId, messageId, content, messageType);
            return result > 0;
        } catch (Exception e) {
            log.error("更新会话最后消息失败", e);
            throw new RuntimeException("更新会话最后消息失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean incrementUnreadCount(Long conversationId, Long fromUserId, Long toUserId) {
        try {
            Conversation conversation = conversationMapper.selectById(conversationId);
            if (conversation == null) {
                return false;
            }

            // 判断发送方和接收方
            if (conversation.getSenderUserId().equals(fromUserId) &&
                    conversation.getReceiveUserId().equals(toUserId)) {
                // 发送方是sender，接收方是receiver
                int result = conversationMapper.incrementReceiverUnread(conversationId);
                return result > 0;
            } else if (conversation.getSenderUserId().equals(toUserId) &&
                    conversation.getReceiveUserId().equals(fromUserId)) {
                // 发送方是receiver，接收方是sender
                int result = conversationMapper.incrementSenderUnread(conversationId);
                return result > 0;
            }

            return false;

        } catch (Exception e) {
            log.error("增加未读计数失败", e);
            throw new RuntimeException("增加未读计数失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean resetUnreadCount(Long conversationId, Long userId) {
        try {
            Conversation conversation = conversationMapper.selectById(conversationId);
            if (conversation == null) {
                log.warn("重置未读计数失败：会话不存在, conversationId={}, userId={}", conversationId, userId);
                return false;
            }
            // 验证用户是否是会话成员
            if (!conversation.getSenderUserId().equals(userId) && !conversation.getReceiveUserId().equals(userId)) {
                log.warn("重置未读计数失败：用户{}不是会话{}的成员, userId={}", userId, conversationId, userId);
                return false;
            }
            // 重置指定用户的未读计数
            int result = conversationMapper.resetUnreadCount(conversationId, userId);
            if (result > 0) {
                log.info("用户{}的会话{}未读计数重置成功", userId, conversationId);
                return true;
            } else {
                log.info("用户{}的会话{}未读计数已经是0", userId, conversationId);
                return true; // 未读计数已经是0，也算成功
            }
        } catch (Exception e) {
            log.error("重置未读计数失败, conversationId={}, userId={}", conversationId, userId, e);
            throw new RuntimeException("重置未读计数失败: " + e.getMessage());
        }
    }
    private Long choice(Long userId,Conversation conversation){
        if (userId.equals(conversation.getSenderUserId())) {
            return conversation.getReceiveUserId();
        }else {
            return conversation.getSenderUserId();
        }
    }
    private UserInfoVo getUserInfoVo(Long userId, Conversation conversation) {
        if (userId.equals(conversation.getSenderUserId())){
            RpcResult<UserInfoVo> rpcResult = userDubboService.getUserInfo(conversation.getReceiveUserId());
            return rpcResult.getData();
        }
        RpcResult<UserInfoVo> rpcResult = userDubboService.getUserInfo(conversation.getReceiveUserId());
        return rpcResult.getData();
    }
}
