package com.unlimited.sports.globox.social.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.IdGenerator;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.model.social.dto.MessageDto;
import com.unlimited.sports.globox.model.social.entity.*;
import com.unlimited.sports.globox.model.social.vo.MessageListVo;
import com.unlimited.sports.globox.model.social.vo.MessageVo;
import com.unlimited.sports.globox.social.mapper.ConversationMapper;
import com.unlimited.sports.globox.social.mapper.MessageMapper;
import com.unlimited.sports.globox.social.service.ConversationService;
import com.unlimited.sports.globox.social.service.MessageService;
import com.unlimited.sports.globox.social.util.TencentCloudImUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 消息服务实现类
 */
@Slf4j
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, MessageEntity> implements MessageService {

    @Autowired
    private TencentCloudImUtil tencentCloudImUtil;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired(required = false)  // 本地环境可能不存在，设为可选
    private MessageProducerService messageProducerService;

    @DubboReference(group = "rpc")
    private UserDubboService userDubboService;


    private static final String REDIS_MESSAGE_QUEUE = "silence:im:message:queue:entity:";
    private static final String REDIS_MESSAGE_KEY = "silence:im:message:entity:";
    private static final long MESSAGE_TTL = 86400; // 24小时
    @Autowired
    private ConversationMapper conversationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String sendMessage(MessageDto messageDto, Long conversationId) {
        try {
            // 1. 生成消息随机值
            Long msgRandom = RandomUtils.nextLong(0, 999999999);
            
            // 2. 调用腾讯IM发送消息（同步调用，确保消息真实发送成功）
            TencentImResult result = tencentCloudImUtil.sendMsgSync(
                1, // 同步到发送方
                messageDto.getFromUserId().toString(),
                messageDto.getToUserId().toString(),
                getTencentMsgType(messageDto.getMessageType().getCode()),
                messageDto.getContent()
            );

            if (!result.isSuccess()) {
                log.error("发送消息失败: {}", result.getErrorMessage());
                return MessageResult.FAILURE.getMessage() + result.getErrorMessage();
            }

            // 3. 保存消息到Redis缓存（快速响应）
            MessageEntity messageEntity = convertToMessage(messageDto, result, msgRandom, conversationId);
            saveMessageToRedis(messageEntity);

            // 4. 同步保存到数据库（确保会话能立即更新）
            saveMessageToDB(messageEntity);
            log.info("消息已同步保存到数据库，消息ID: {}", messageEntity.getMessageId());

            // 5. 同步更新会话（确保会话立即更新）
            updateConversationAfterMessage(messageEntity, conversationId);

            log.info("消息发送成功，消息ID: {}", messageEntity.getMessageId());
            return MessageResult.SUCCESS.getMessage();

        } catch (Exception e) {
            log.error("发送消息异常", e);
            throw new RuntimeException("发送消息异常: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String batchSendMessage(Long fromUserId, List<Long> toUserIds, MessageDto messageDto) {
        try {
            Long msgRandom = RandomUtils.nextLong(0, 999999999);
            
            List<String> toUserIdsStr = new ArrayList<>();
            for (Long userId : toUserIds) {
                toUserIdsStr.add(userId.toString());
            }

            // 调用腾讯IM批量发送消息
            String result = tencentCloudImUtil.batchSendMsg(
                1,
                fromUserId.toString(),
                toUserIdsStr,
                getTencentMsgType(messageDto.getMessageType().getCode()),
                messageDto.getContent()
            );

            log.info("批量发送消息结果: {}", result);

            // 收集所有消息事件，批量发送到队列
            List<MessageQueueEvent> events = new ArrayList<>();
            
            for (Long toUserId : toUserIds) {
                // 使用雪花算法生成消息ID
                long messageId = idGenerator.nextId();
                
                // 获取或创建会话
                Conversation conversation = conversationService.getOrCreateConversation(fromUserId, toUserId);
                
                // 构建消息实体
                MessageEntity messageEntity = MessageEntity.builder()
                    .messageId(messageId)
                    .fromUserId(fromUserId)
                    .toUserId(toUserId)
                    .messageType(messageDto.getMessageType())
                    .content(messageDto.getContent())
                    .status(MessageStatusEnum.SENT)
                    .isRead(false)
                    .sendTime(LocalDateTime.now())
                    .random(msgRandom)
                    .conversationId(conversation.getConversationId())
                    .extra(messageDto.getExtra())
                    .build();
                
                // 保存到Redis缓存
                saveMessageToRedis(messageEntity);
                
                // 同步保存到数据库
                saveMessageToDB(messageEntity);
                
                // 同步更新会话
                updateConversationAfterMessage(messageEntity, conversation.getConversationId());
                
                // 构建消息队列事件（用于其他异步处理）
                MessageQueueEvent event = MessageQueueEvent.builder()
                    .messageId(messageEntity.getMessageId())
                    .fromUserId(messageEntity.getFromUserId())
                    .toUserId(messageEntity.getToUserId())
                    .messageType(messageEntity.getMessageType())
                    .content(messageEntity.getContent())
                    .status(messageEntity.getStatus())
                    .isRead(messageEntity.getIsRead())
                    .sendTime(messageEntity.getSendTime())
                    .conversationId(messageEntity.getConversationId())
                    .random(messageEntity.getRandom())
                    .extra(messageEntity.getExtra())
                    .operationType("SAVE_MESSAGE")
                    .build();
                
                events.add(event);
                
                log.info("批量发送消息到用户{}完成，会话ID: {}", toUserId, conversation.getConversationId());
            }
            
            // 批量发送到消息队列（本地环境可能不存在 messageProducerService）
            if (!events.isEmpty() && messageProducerService != null) {
                messageProducerService.sendBatchMessageToQueue(events);
            }
            
            log.info("批量消息已发送到队列，数量: {}", events.size());
            return "批量发送消息成功";

        } catch (Exception e) {
            log.error("批量发送消息异常", e);
            throw new RuntimeException("批量发送消息异常: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String recallMessage(Long messageId, Long userId) {
        try {
            MessageEntity messageEntity = messageMapper.selectById(messageId);
            if (messageEntity == null) {
                return MessageResult.MESSAGE_IS_NULL.getMessage();
            }

            if (!messageEntity.getFromUserId().equals(userId)) {
                return MessageResult.NOT_PERMISSION_TO_RECALL_OTHERS_MESSAGES.getMessage();
            }

            if (messageEntity.getSendTime() != null) {
                long minutes = java.time.Duration.between(messageEntity.getSendTime(), LocalDateTime.now()).toMinutes();
                if (minutes > 2) {
                    return MessageResult.OVER_RECALL_TIME_LIMIT.getMessage();
                }
            }

            tencentCloudImUtil.adminMsgWithDraw(
                messageEntity.getFromUserId().toString(),
                messageEntity.getToUserId().toString(),
                messageEntity.getMessageId().toString()
            );

            messageEntity.setStatus(MessageStatusEnum.RECALLED);
            messageEntity.setIsRecalled(true);
            messageEntity.setRecalledAt(LocalDateTime.now());
            messageEntity.setUpdatedAt(LocalDateTime.now());
            
            messageMapper.updateById(messageEntity);
            updateMessageInRedis(messageEntity);

            return MessageResult.MESSAGE_RECALL_SUCCESS.getMessage();

        } catch (Exception e) {
            log.error("撤回消息异常", e);
            throw new RuntimeException("撤回消息异常: " + e.getMessage());
        }
    }

    @Override
    public List<MessageEntity> queryChatMessages(Long fromUserId, Long toUserId, Integer maxCnt, Long startTime, Long endTime) {
        try {
            // 先从Redis查询
            List<MessageEntity> redisMessageEntities = queryMessagesFromRedis(fromUserId, toUserId, maxCnt, startTime, endTime);
            
            if (redisMessageEntities.size() >= maxCnt) {
                // 过滤撤回的消息
                redisMessageEntities.removeIf(message -> message.getIsRecalled() != null && message.getIsRecalled());
                return redisMessageEntities;
            }

            // 从腾讯IM查询历史消息
            String result = tencentCloudImUtil.adminGetRoamMsg(
                fromUserId.toString(),
                toUserId.toString(),
                maxCnt,
                startTime,
                endTime,
                null
            );

            log.info("查询腾讯IM消息结果: {}", result);

            // 解析并保存到数据库
            List<MessageEntity> messageEntities = parseAndSaveMessages(result, fromUserId, toUserId);
            
            // 合并结果
            if (redisMessageEntities.size() > 0) {
                messageEntities.addAll(0, redisMessageEntities);
            }

            // 过滤撤回的消息
            messageEntities.removeIf(message -> message.getIsRecalled() != null && message.getIsRecalled());

            return messageEntities;

        } catch (Exception e) {
            log.error("查询消息异常", e);
            throw new RuntimeException("查询消息异常: " + e.getMessage());
        }
    }

    @Override
    public Integer getC2CUnreadMsgNum(Long fromUserId, Long toUserId) {
        try {
            return tencentCloudImUtil.getC2CUnreadMsgNum(
                fromUserId.toString(),
                toUserId.toString()
            );
        } catch (Exception e) {
            log.error("查询未读消息计数异常", e);
            return 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deleteMessage(Long messageId, Long userId) {
        try {
            MessageEntity messageEntity = messageMapper.selectById(messageId);
            log.info("消息{}",messageEntity);
            if (messageEntity == null) {
                log.info("消息为空");
                return MessageResult.MESSAGE_IS_NULL.getMessage();
            }

            if (messageEntity.getFromUserId().equals(userId)) {
                messageEntity.setIsDeletedBySender(true);
            } else if (messageEntity.getToUserId().equals(userId)) {
                messageEntity.setIsDeletedByReceiver(true);
            } else {
                log.info("你没有权限删除");
                return MessageResult.NOT_PERMISSION_TO_DELETE_OTHERS_MESSAGES.getMessage();
            }

            messageEntity.setStatus(MessageStatusEnum.DELETED);
            messageEntity.setUpdatedAt(LocalDateTime.now());
            
            messageMapper.updateById(messageEntity);
            updateMessageInRedis(messageEntity);

            log.info("--------{}",MessageResult.MESSAGE_DELETE_SUCCESS.getMessage());
            return MessageResult.MESSAGE_DELETE_SUCCESS.getMessage();

        } catch (Exception e) {
            log.error("删除消息异常", e);
            throw new RuntimeException("删除消息异常: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importMessageToDB(MessageEntity messageEntity) {
        try {
            if (messageEntity.getRandom() != null) {
                MessageEntity existing = messageMapper.selectByRandom(
                    messageEntity.getFromUserId(),
                    messageEntity.getToUserId(),
                    messageEntity.getRandom()
                );
                if (existing != null) {
                    return MessageResult.MESSAGE_HAS_EXISTED.getMessage();
                }
            }

            // 如果消息没有ID，使用雪花算法生成
            if (messageEntity.getMessageId() == null) {
                long messageId = idGenerator.nextId();
                messageEntity.setMessageId(messageId);
            }

            messageMapper.insert(messageEntity);
            return MessageResult.MESSAGE_IMPORT_SUCCESS.getMessage();

        } catch (Exception e) {
            log.error("导入消息异常", e);
            throw new RuntimeException("导入消息异常: " + e.getMessage());
        }
    }

    @Override
    public Integer syncMessageFromRedis(Long fromUserId, Long toUserId) {
        try {
            String redisKey = REDIS_MESSAGE_QUEUE + fromUserId + "_" + toUserId;
            List<MessageEntity> messageEntities = (List<MessageEntity>) redisTemplate.opsForValue().get(redisKey);
            
            if (messageEntities == null || messageEntities.size() == 0) {
                return 0;
            }

            int count = 0;
            for (MessageEntity messageEntity : messageEntities) {
                try {
                    // 检查是否已存在
                    MessageEntity existing = messageMapper.selectById(messageEntity.getMessageId());
                    if (existing == null) {
                        messageMapper.insert(messageEntity);
                        count++;
                    }
                } catch (Exception e) {
                    log.error("同步消息到数据库失败: {}", messageEntity.getMessageId(), e);
                }
            }

            redisTemplate.delete(redisKey);
            return count;

        } catch (Exception e) {
            log.error("同步消息从Redis异常", e);
            return 0;
        }
    }



    private MessageEntity convertToMessage(MessageDto dto, TencentImResult result, Long msgRandom, Long conversationId) {
        // 使用雪花算法生成消息ID
        long messageId = idGenerator.nextId();
        
        MessageEntity messageEntity = MessageEntity.builder()
            .messageId(messageId)
            .fromUserId(dto.getFromUserId())
            .toUserId(dto.getToUserId())
            .messageType(dto.getMessageType())
            .content(dto.getContent())
            .status(MessageStatusEnum.SENT)
            .isRead(false)
            .sendTime(LocalDateTime.now())
            .random(msgRandom)
            .conversationId(conversationId)
            .extra(dto.getExtra()).build();
        
        if (result != null && result.getMsgKey() != null) {
            String extra = dto.getExtra() != null ? dto.getExtra() : "{}";
            messageEntity.setExtra(extra.replace("}", ",\"tencentMsgKey\":\"" + result.getMsgKey() + "\"}"));
        }
        
        return messageEntity;
    }

    private void saveMessageToRedis(MessageEntity messageEntity) {
        try {
            String messageKey = REDIS_MESSAGE_KEY + messageEntity.getMessageId();
            redisTemplate.opsForValue().set(messageKey, messageEntity, MESSAGE_TTL, TimeUnit.SECONDS);

            String queueKey = REDIS_MESSAGE_QUEUE + messageEntity.getFromUserId() + "_" + messageEntity.getToUserId();
            List<MessageEntity> queue = (List<MessageEntity>) redisTemplate.opsForValue().get(queueKey);
            if (queue == null) {
                queue = new ArrayList<>();
            }
            queue.add(messageEntity);
            redisTemplate.opsForValue().set(queueKey, queue, MESSAGE_TTL, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("保存消息到Redis异常", e);
        }
    }

    private void updateMessageInRedis(MessageEntity messageEntity) {
        try {
            String messageKey = REDIS_MESSAGE_KEY + messageEntity.getMessageId();
            redisTemplate.opsForValue().set(messageKey, messageEntity, MESSAGE_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("更新Redis消息异常", e);
        }
    }

    private void saveMessageToDB(MessageEntity messageEntity) {
        try {
            if (messageEntity.getMessageId() != null) {
                MessageEntity existing = messageMapper.selectById(messageEntity.getMessageId());
                if (existing != null) {
                    messageMapper.updateById(messageEntity);
                    return;
                }
            }
            messageMapper.insert(messageEntity);
        } catch (Exception e) {
            log.error("保存消息到数据库异常", e);
            throw e;
        }
    }

    private void updateConversationAfterMessage(MessageEntity messageEntity, Long conversationId) {
        try {
            // 更新会话的最后消息信息
            conversationService.updateLastMessage(
                conversationId,
                messageEntity.getMessageId(),
                messageEntity.getContent(),
                messageEntity.getMessageType()
            );
            
            // 只有当消息状态为未读时，才增加接收方的未读计数
            if (!messageEntity.getIsRead() && messageEntity.getStatus() != MessageStatusEnum.READ) {
                conversationService.incrementUnreadCount(
                    conversationId,
                    messageEntity.getFromUserId(),
                    messageEntity.getToUserId()
                );
                log.info("消息未读，增加接收方未读计数，conversationId: {}", conversationId);
            } else {
                log.info("消息已读，不增加未读计数，conversationId: {}", conversationId);
            }
            
            log.info("会话更新成功，conversationId: {}", conversationId);
        } catch (Exception e) {
            log.error("更新会话最后消息异常", e);
            // 这里不抛出异常，因为消息已经发送成功，只是会话更新失败
        }
    }

    private List<MessageEntity> queryMessagesFromRedis(Long fromUserId, Long toUserId, Integer maxCnt, Long startTime, Long endTime) {
        List<MessageEntity> result = new ArrayList<>();
        
        try {
            String queueKey = REDIS_MESSAGE_QUEUE + fromUserId + "_" + toUserId;
            List<MessageEntity> queue = (List<MessageEntity>) redisTemplate.opsForValue().get(queueKey);
            
            if (queue != null) {
                for (MessageEntity messageEntity : queue) {
                    if (startTime != null && messageEntity.getSendTime() != null) {
                        if (messageEntity.getSendTime().toEpochSecond(java.time.ZoneOffset.UTC) < startTime) {
                            continue;
                        }
                    }
                    if (endTime != null && messageEntity.getSendTime() != null) {
                        if (messageEntity.getSendTime().toEpochSecond(java.time.ZoneOffset.UTC) > endTime) {
                            continue;
                        }
                    }
                    
                    result.add(messageEntity);
                    
                    if (result.size() >= maxCnt) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("从Redis查询消息异常", e);
        }
        
        return result;
    }

    private List<MessageEntity> parseAndSaveMessages(String result, Long fromUserId, Long toUserId) {
        List<MessageEntity> messageEntities = new ArrayList<>();

        try {
            if (result == null || result.trim().isEmpty()) {
                return messageEntities;
            }

            // 使用Jackson解析腾讯IM返回的JSON数据
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(result);

            // 获取消息列表节点（根据腾讯IM实际返回格式调整）
            com.fasterxml.jackson.databind.JsonNode msgListNode = rootNode.path("MsgList");
            if (msgListNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode msgNode : msgListNode) {
                    MessageEntity messageEntity = MessageEntity.builder().build();

                    // 解析消息基本信息
                    com.fasterxml.jackson.databind.JsonNode fromAccountNode = msgNode.path("From_Account");
                    com.fasterxml.jackson.databind.JsonNode toAccountNode = msgNode.path("To_Account");
                    com.fasterxml.jackson.databind.JsonNode msgBodyNode = msgNode.path("MsgBody");

                    // 设置发送方和接收方
                    messageEntity.setFromUserId(Long.valueOf(fromAccountNode.asText()));
                    messageEntity.setToUserId(Long.valueOf(toAccountNode.asText()));

                    // 解析消息内容和类型
                    if (msgBodyNode.isArray() && msgBodyNode.size() > 0) {
                        com.fasterxml.jackson.databind.JsonNode body = msgBodyNode.get(0);
                        String msgType = body.path("MsgType").asText();
                        messageEntity.setMessageType(convertTencentMsgTypeToEnum(msgType));

                        // 根据消息类型解析内容
                        com.fasterxml.jackson.databind.JsonNode contentNode = body.path("MsgContent");
                        if (!contentNode.isMissingNode()) {
                            messageEntity.setContent(contentNode.path("Text").asText());
                        }
                    }

                    // 设置消息状态和时间
                    messageEntity.setStatus(MessageStatusEnum.SENT);
                    messageEntity.setIsRead(false);

                    // 解析时间戳
                    long time = msgNode.path("Time").asLong();
                    messageEntity.setSendTime(LocalDateTime.ofEpochSecond(time, 0, java.time.ZoneOffset.UTC));

                    // 使用雪花算法生成消息ID
                    long messageId = idGenerator.nextId();
                    messageEntity.setMessageId(messageId);
                    
                    // 保存到数据库
                    messageMapper.insert(messageEntity);
                    messageEntities.add(messageEntity);
                }
            }

        } catch (Exception e) {
            log.error("解析腾讯IM消息异常", e);
        }

        return messageEntities;
    }


    @Override
    public MessageEntity getMessageDetail(Long messageId) {
        try {
            MessageEntity messageEntity = messageMapper.selectByIdNotDeleted(messageId);
            return messageEntity;
        } catch (Exception e) {
            log.error("查询消息详情异常", e);
            throw new RuntimeException("查询消息详情异常: " + e.getMessage());
        }
    }

    @Override
    public MessageListVo getMessageListByConversation(Long conversationId, Integer page, Integer pageSize, Long userId) {
        try {
            Integer offset = (page - 1) * pageSize;
            List<MessageEntity> messageEntities = messageMapper.selectByConversationId(conversationId, offset, pageSize);
            // 过滤撤回的消息
            messageEntities.removeIf(message -> message.getIsRecalled() != null && message.getIsRecalled());
            
            // 查询总条数
            Long total = messageMapper.selectCountByConversationId(conversationId);
            List<MessageVo> messageVos = messageEntities.stream()
                    .map(messageEntity -> MessageVo.builder()
                        .messageId(messageEntity.getMessageId())
                        .fromUserId(messageEntity.getFromUserId())
                        .fromUserName(userDubboService.getUserInfo(messageEntity.getFromUserId()).getNickName())
                        .fromUserAvatar(userDubboService.getUserInfo(messageEntity.getFromUserId()).getAvatarUrl())
                        .toUserId(messageEntity.getToUserId())
                        .toUserName(userDubboService.getUserInfo(messageEntity.getToUserId()).getNickName())
                        .toUserAvatar(userDubboService.getUserInfo(messageEntity.getToUserId()).getAvatarUrl())
                        .messageType(messageEntity.getMessageType())
                        .content(messageEntity.getContent())
                        .status(messageEntity.getStatus())
                        .isRead(messageEntity.getIsRead())
                        .sendTime(messageEntity.getSendTime())
                        .receiveTime(messageEntity.getReceiveTime())
                        .readTime(messageEntity.getReadTime())
                        .conversationId(messageEntity.getConversationId())
                        .random(messageEntity.getRandom())
                        .isRecalled(messageEntity.getIsRecalled())
                        .isDeletedBySender(messageEntity.getIsDeletedBySender())
                        .isDeletedByReceiver(messageEntity.getIsDeletedByReceiver())
                        .extra(messageEntity.getExtra())
                        .createdAt(messageEntity.getCreatedAt())
                        .updatedAt(messageEntity.getUpdatedAt())
                        .build()).toList();
            log.info("查询会话消息列表成功{}",messageVos);
            Conversation conversation = conversationMapper.selectByConversationId(conversationId);
            Long friendId = null;
            if (userId.equals(conversation.getSenderUserId())){
                friendId = conversation.getReceiveUserId();
            }else {
                friendId = conversation.getSenderUserId();
            }
            UserInfoVo userInfo = userDubboService.getUserInfo(friendId);
            MessageListVo messageListVo = MessageListVo.builder()
                    .messageVoList(messageVos)
                    .total( total)
                    .page(page)
                    .pageSize(pageSize)
                    .name(userInfo.getNickName())
                    .avatar(userInfo.getAvatarUrl())
                    .build();
            return messageListVo;
        } catch (Exception e) {
            log.error("查询会话消息列表异常", e);
            throw new RuntimeException("查询会话消息列表异常: " + e.getMessage());
        }
    }


    private String getTencentMsgType(Integer messageType) {
        switch (messageType) {
            case 1: return "TIMTextElem";
            case 2: return "TIMImageElem";
            case 3: return "TIMSoundElem";
            case 4: return "TIMVideoElem";
            case 5: return "TIMFileElem";
            case 6: return "TIMLocationElem";
            default: return "TIMTextElem";
        }
    }
    private MessageTypeEnum convertTencentMsgTypeToEnum(String tencentMsgType) {
        switch (tencentMsgType) {
            case "TIMTextElem":
                return MessageTypeEnum.TEXT; // TEXT
            case "TIMImageElem":
                return MessageTypeEnum.IMAGE; // IMAGE
            case "TIMSoundElem":
                return MessageTypeEnum.AUDIO; // AUDIO
            case "TIMVideoElem":
                return MessageTypeEnum.VIDEO; // VIDEO
            case "TIMFileElem":
                return MessageTypeEnum.FILE; // FILE
            case "TIMLocationElem":
                return MessageTypeEnum.LOCATION; // LOCATION
            default:
                return MessageTypeEnum.TEXT; // 默认为文本消息
        }
    }

}
