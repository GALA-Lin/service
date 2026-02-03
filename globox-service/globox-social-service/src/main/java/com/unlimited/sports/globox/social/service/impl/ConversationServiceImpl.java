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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        int offset = (page - 1) * pageSize;
        List<Conversation> pagedConversations =
                conversationMapper.selectByUserIdWithPagination(userId, offset, pageSize);

        List<ConversationVo> conversationVoList = pagedConversations.stream()
                .filter(conversation -> {
                    // 验证用户是否在会话中
                    return conversation.getPeerUserId(userId) != null;
                })
                .flatMap(conversation -> {
                    try {
                        // 获取对方用户ID
                        Long peerUserId = conversation.getPeerUserId(userId);

                        // 获取对方用户信息
                        RpcResult<UserInfoVo> rpcResult = userDubboService.getUserInfo(peerUserId);
                        Assert.rpcResultOk(rpcResult);
                        UserInfoVo peerUserInfo = rpcResult.getData();

                        // 获取当前用户的未读数
                        Long unreadCount = conversation.getUnreadCountForUser(userId);

                        ConversationVo vo = ConversationVo.builder()
                                .conversationId(String.valueOf(conversation.getConversationId()))
                                .conversationType(conversation.getConversationType())
                                .receiveUserId(peerUserId)
                                .conversationNameReceiver(peerUserInfo.getNickName())
                                .conversationAvatarReceiver(peerUserInfo.getAvatarUrl())
                                .unreadCountSender(conversation.getUnreadCountSender())
                                .unreadCountReceiver(conversation.getUnreadCountReceiver())
                                .isBlocked(conversation.getIsBlocked())
                                .isPinnedSender(conversation.getIsPinnedSender())
                                .isPinnedReceiver(conversation.getIsPinnedReceiver())
                                .isDeletedSender(conversation.getIsDeletedSender())
                                .isDeletedReceiver(conversation.getIsDeletedReceiver())
                                .lastMessageId(conversation.getLastMessageId())
                                .lastMessageContent(conversation.getLastMessageContent())
                                .lastMessageType(conversation.getLastMessageType())
                                .lastMessageAt(conversation.getLastMessageAt())
                                .createdAt(conversation.getCreatedAt())
                                .updatedAt(conversation.getUpdatedAt())
                                .build();

                        return Stream.of(vo);
                    } catch (Exception e) {
                        log.error("构建 ConversationVo 失败，conversationId={}, userId={}",
                                conversation.getConversationId(), userId, e);
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());

        Long total = conversationMapper.countByUserId(userId);

        return PaginationResult.build(conversationVoList, total != null ? total : 0L, page, pageSize);
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
                    page,
                    pageSize
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
                return null;
            }
            // 先检查会话是否已存在
            Conversation existing = conversationMapper.selectByUserPair(userId, friendId);
            if (existing != null) {
//                resetUnreadCount(existing.getConversationId(), userId);
                return existing;
            }

            // 会话不存在，创建新会话
            RpcResult<UserInfoVo> rpcResult = userDubboService.getUserInfo(friendId);
            Assert.rpcResultOk(rpcResult);
            UserInfoVo friendInfo = rpcResult.getData();

            RpcResult<UserInfoVo> rpcResult2 = userDubboService.getUserInfo(userId);
            Assert.rpcResultOk(rpcResult2);
            UserInfoVo userInfo = rpcResult2.getData();

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
            // 获取用户的所有会话
            List<Conversation> conversations = conversationMapper.selectByUserId(userId);

            if (conversations == null || conversations.isEmpty()) {
                return true;
            }

            long totalCleared = 0;
            long totalMessagesMarked = 0;

            for (Conversation conversation : conversations) {
                try {
                    // 获取对方用户ID
                    Long peerUserId = conversation.getPeerUserId(userId);
                    if (peerUserId == null) {
                        log.warn("用户{}不在会话{}中，跳过", userId, conversation.getConversationId());
                        continue;
                    }

                    // 查询对方发给我的未读消息
                    List<MessageEntity> unreadMessages = messageMapper.selectUnreadMessages(
                            peerUserId,  // 发送方：对方
                            userId       // 接收方：我
                    );

                    // 标记消息为已读
                    if (unreadMessages != null && !unreadMessages.isEmpty()) {
                        for (MessageEntity message : unreadMessages) {
                            if (!message.getIsRead()) {
                                message.setIsRead(true);
                                message.setStatus(MessageStatusEnum.READ);
                                message.setReadTime(LocalDateTime.now());
                                message.setUpdatedAt(LocalDateTime.now());
                                messageMapper.updateById(message);
                                totalMessagesMarked++;
                            }
                        }
                        log.info("会话{}下的{}条未读消息已标记为已读",
                                conversation.getConversationId(), unreadMessages.size());
                    }

                    // 清除会话未读计数
                    int cleared = conversationMapper.clearUnreadCount(conversation.getConversationId(), userId);
                    if (cleared > 0) {
                        totalCleared++;
                    }

                } catch (Exception e) {
                    log.error("清除会话{}的未读计数失败", conversation.getConversationId(), e);
                }
            }

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
        log.info("开始标记会话已读: conversationId={}, userId={}", conversationId, userId);
        try {
            Conversation conversation = conversationMapper.selectById(conversationId);
            if (conversation == null) {
                log.info("会话不存在: {}", conversationId);
                return false;
            }

            // 获取对方用户ID
            Long peerUserId = conversation.getPeerUserId(userId);
            if (peerUserId == null) {
                log.error("用户{}不在会话{}中", userId, conversationId);
                return false;
            }

            // 标记会话已读（清除未读计数）
            int result = conversationMapper.markConversationRead(conversationId, userId);

            // 查询对方发给我的未读消息
            List<MessageEntity> unreadMessages = messageMapper.selectUnreadMessages(
                    peerUserId,  // 发送方：对方
                    userId       // 接收方：我
            );

            // 将未读消息标记为已读
            if (unreadMessages != null && !unreadMessages.isEmpty()) {
                for (MessageEntity message : unreadMessages) {
                    if (!message.getIsRead() && message.getToUserId().equals(userId)) {
                        message.setIsRead(true);
                        message.setStatus(MessageStatusEnum.READ);
                        message.setReadTime(LocalDateTime.now());
                        message.setUpdatedAt(LocalDateTime.now());
                        messageMapper.updateById(message);
                    }
                }
                log.info("会话{}下的{}条未读消息已标记为已读", conversationId, unreadMessages.size());
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
                log.error("会话不存在: {}", conversationId);
                return false;
            }

            // 使用辅助方法判断接收方角色
            if (conversation.isSenderRole(toUserId)) {
                // 接收方是 sender 角色，增加 sender 的未读
                int result = conversationMapper.incrementSenderUnread(conversationId);
                log.info("增加sender未读计数: conversationId={}, result={}", conversationId, result);
                return result > 0;
            } else if (conversation.getPeerUserId(toUserId) != null) {
                // 接收方是 receiver 角色，增加 receiver 的未读
                int result = conversationMapper.incrementReceiverUnread(conversationId);
                log.info("增加receiver未读计数: conversationId={}, result={}", conversationId, result);
                return result > 0;
            }

            log.error("用户{}不在会话{}中", toUserId, conversationId);
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
                return false;
            }
            // 验证用户是否是会话成员
            if (!conversation.getSenderUserId().equals(userId) && !conversation.getReceiveUserId().equals(userId)) {
                return false;
            }
            // 重置指定用户的未读计数
            int result = conversationMapper.resetUnreadCount(conversationId, userId);
            if (result > 0) {
                return true;
            } else {
                return true;
            }
        } catch (Exception e) {
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
        RpcResult<UserInfoVo> rpcResult = userDubboService.getUserInfo(conversation.getSenderUserId());
        return rpcResult.getData();
    }
}
