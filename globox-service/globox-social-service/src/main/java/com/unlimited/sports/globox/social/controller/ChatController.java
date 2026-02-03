package com.unlimited.sports.globox.social.controller;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.ApplicationCode;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.dubbo.governance.SensitiveWordsDubboService;
import com.unlimited.sports.globox.model.social.dto.MessageDto;
import com.unlimited.sports.globox.model.social.entity.*;
import com.unlimited.sports.globox.model.social.vo.ConversationVo;
import com.unlimited.sports.globox.model.social.vo.MessageListVo;
import com.unlimited.sports.globox.model.social.vo.MessageVo;
import com.unlimited.sports.globox.social.service.ConversationService;
import com.unlimited.sports.globox.social.service.MessageService;
import com.unlimited.sports.globox.social.service.TencentCloudImService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_USER_ID;
import static com.unlimited.sports.globox.common.result.UserAuthCode.TOKEN_EXPIRED;

/**
 * Globox单聊系统控制器
 */
@Slf4j
@RestController
@RequestMapping("/social/chat")
@Tag(name = "聊天模块", description = "单聊、会话、消息相关接口")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    @Autowired
    private TencentCloudImService tencentCloudImService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ConversationService conversationService;

    @DubboReference(group = "rpc")
    private SensitiveWordsDubboService sensitiveWordsDubboService;


    /**
     * 获取用户签名
     * GET /social/chat/user/sig
     */
    @GetMapping("/user/sig")
    @Operation(summary = "获取腾讯IM用户签名", description = "获取腾讯云IM用户签名，用于初始化IM SDK")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<Map<String, Object>> getUserSig(
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        try {
            String userSig = tencentCloudImService.getTxCloudUserSig(String.valueOf(userId));
            Map<String, Object> result = new HashMap<>();
            result.put("userSig", userSig);
            result.put("sdkAppId", 1600119377);
            return R.ok(result);
        } catch (Exception e) {
            log.error("获取用户签名失败", e);
            return R.error(ApplicationCode.FAIL);
        }
    }

    /**
     * 获取用户会话列表（带分页和总数）
     * GET /social/chat/conversation/list
     */
    @GetMapping("/conversation/list")
    @Operation(summary = "获取会话列表", description = "获取当前用户的会话列表，支持分页")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<PaginationResult<ConversationVo>> getConversationList(
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "页码（从1开始）", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "50")
            @RequestParam(defaultValue = "50") Integer pageSize) {
        try {
            PaginationResult<ConversationVo> result =
                    conversationService.getConversationVoList(userId, page, pageSize);
            log.info("用户：{},获取用户会话列表:{}", userId, result);
            return R.ok(result);
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return R.error(ApplicationCode.FAIL);
        }
    }

    /**
     * 获取或创建会话 - 如果会话存在则返回，不存在则创建
     * GET /social/chat/conversation/get-or-create
     */
    @GetMapping("/conversation/get-or-create")
    @Operation(summary = "获取或创建会话", description = "如果会话存在则返回，不存在则创建新会话。自己之间不能发消息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取或创建成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<Conversation> getOrCreateConversation(
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "好友用户ID", required = true)
            @RequestParam Long friendId) {
        try {
            if (userId == null) {
                log.error("请求头中缺少{}", HEADER_USER_ID);
                throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
            }
            if (friendId == null) {
                return R.<Conversation>error().message("好友ID不能为空");
            }
            Conversation conversation = conversationService.getOrCreateConversation(userId, friendId);
            if (conversation == null) {
                return R.<Conversation>error().message("自己之间不能发消息");
            }
            return R.ok(conversation);
        } catch (Exception e) {
            log.error("获取或创建会话失败", e);
            return R.error(ApplicationCode.FAIL);
        }
    }


    /**
     * 置顶/取消置顶会话
     * PUT /social/chat/conversation/{conversationId}/pin
     */
    @PutMapping("/conversation/{conversationId}/pin")
    @Operation(summary = "置顶/取消置顶会话", description = "设置会话的置顶状态")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<String> togglePinned(
            @Parameter(description = "会话ID", required = true)
            @PathVariable Long conversationId,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "是否置顶", required = true)
            @RequestParam Boolean isPinned) {
        try {
            if (userId == null) {
                log.error("请求头中缺少{}", HEADER_USER_ID);
                throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
            }
            if (conversationId == null) {
                return R.<String>error().message("会话ID不能为空");
            }
            conversationService.togglePinned(conversationId, userId, isPinned);
            return R.ok(isPinned ? "PINNED" : "UNPINNED");
        } catch (Exception e) {
            log.error("置顶会话失败", e);
            return R.error(ApplicationCode.FAIL);
        }
    }

    /**
     * 屏蔽/取消屏蔽会话
     * PUT /social/chat/conversation/{conversationId}/block
     */
    @PutMapping("/conversation/{conversationId}/block")
    @Operation(summary = "屏蔽/取消屏蔽会话", description = "设置会话的屏蔽状态")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<String> toggleBlocked(
            @Parameter(description = "会话ID", required = true)
            @PathVariable Long conversationId,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "是否屏蔽", required = true)
            @RequestParam Boolean isBlocked) {
        try {
            if (userId == null) {
                log.error("请求头中缺少{}", HEADER_USER_ID);
                throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
            }
            if (conversationId == null) {
                return R.<String>error().message("会话ID不能为空");
            }
            Boolean result = conversationService.toggleBlocked(conversationId, userId, isBlocked);
            return R.ok(isBlocked ? "BLOCKED" : "UNBLOCKED");
        } catch (Exception e) {
            log.error("屏蔽会话失败", e);
            return R.error(ApplicationCode.FAIL);
        }
    }

    /**
     * 清除用户所有会话的未读计数
     * PUT /social/chat/conversation/clear-unread
     */
    @PutMapping("/conversation/clear-unread")
    @Operation(summary = "清除所有会话未读计数", description = "清除当前用户所有会话的未读消息计数")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "清除成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<String> clearUnreadCount(
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        try {
            if (userId == null) {
                log.error("请求头中缺少{}", HEADER_USER_ID);
                throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
            }
            conversationService.clearUnreadCount(userId);
            return R.ok("处理成功");
        } catch (Exception e) {
            log.error("清除未读计数失败", e);
            return R.error(ApplicationCode.FAIL);
        }
    }


    /**
     * 删除会话
     * DELETE /social/chat/conversation/{conversationId}
     */
    @DeleteMapping("/conversation/{conversationId}")
    @Operation(summary = "删除会话", description = "删除指定会话（软删除）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<String> deleteConversation(
            @Parameter(description = "会话ID", required = true)
            @PathVariable Long conversationId,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        try {
            if (userId == null) {
                log.error("请求头中缺少{}", HEADER_USER_ID);
                throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
            }
            Boolean result = conversationService.deleteConversation(conversationId, userId);
            if (result) {
                return R.ok("DELETED");
            }
            return R.error(ApplicationCode.FAIL);

        } catch (Exception e) {
            log.error("删除会话失败", e);
            return R.error(ApplicationCode.FAIL);
        }
    }


    /**
     * 发送消息
     * POST /social/chat/message/send
     */
    @PostMapping("/message/send")
    @Operation(summary = "发送消息", description = "发送单聊消息，支持文本、图片、视频等类型。消息内容会进行敏感词过滤。屏蔽状态下无法发送消息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "发送成功"),
            @ApiResponse(responseCode = "8004", description = "存在敏感词，请修改后重试"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<Map<String, Object>> sendMessage(
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long currentUserId,
            @Parameter(description = "消息内容", required = true)
            @RequestBody MessageDto messageDto) {
        RpcResult<Void> voidRpcResult = sensitiveWordsDubboService.checkSensitiveWords(messageDto.getContent());
        Assert.rpcResultOk(voidRpcResult);
        // 从header获取当前用户ID，覆盖messageDto中的fromUserId
        messageDto.setFromUserId(currentUserId);
        // 1. 获取或创建会话）
        Conversation conversation = conversationService.getOrCreateConversation(
                messageDto.getFromUserId(),
                messageDto.getToUserId()
        );
        if (conversation == null) {
            return R.<Map<String, Object>>error().message("自己之间不能发消息");
        }
        // 2. 检查会话是否被屏蔽
        if (conversation.getIsBlocked()) {
            Long blockedByUserId = conversation.getBlockedByUserId();

            // 如果当前发送方是屏蔽发起者
            if (blockedByUserId != null && blockedByUserId.equals(messageDto.getFromUserId())) {
                log.warn("用户{}尝试向已屏蔽的用户{}发送消息",
                        messageDto.getFromUserId(), messageDto.getToUserId());
                return R.<Map<String, Object>>error().message("你已经屏蔽对方，无法发送消息");
            }

            // 如果当前发送方是被屏蔽者
            if (blockedByUserId != null && !blockedByUserId.equals(messageDto.getFromUserId())) {
                log.warn("用户{}尝试向屏蔽了TA的用户{}发送消息",
                        messageDto.getFromUserId(), messageDto.getToUserId());
                return R.<Map<String, Object>>error().message("你已被对方屏蔽，无法发送消息");
            }
        }

        // 3. 发送消息
        String result = messageService.sendMessage(messageDto, conversation.getConversationId());

        if (result.equals("消息发送成功")) {
            // 从数据库查询消息详情
            List<MessageEntity> messageEntities = messageService.queryChatMessages(
                    messageDto.getFromUserId(),
                    messageDto.getToUserId(),
                    1,
                    null,
                    null
            );

            MessageEntity messageEntity = null;
            if (messageEntities != null && !messageEntities.isEmpty()) {
                messageEntity = messageEntities.get(0);
            }

            if (messageEntity == null) {
                // 如果查询不到，返回临时消息对象（包含发送方信息）
                messageEntity = MessageEntity.builder()
                        .fromUserId(messageDto.getFromUserId())
                        .toUserId(messageDto.getToUserId())
                        .messageType(messageDto.getMessageType())
                        .content(messageDto.getContent())
                        .status(MessageStatusEnum.SENT)
                        .isRead(false)
                        .sendTime(LocalDateTime.now())
                        .conversationId(conversation.getConversationId())
                        .build();
            }

            // 4. 返回消息和会话ID
            Map<String, Object> response = new HashMap<>();
            response.put("message", messageEntity);
            response.put("conversationId", conversation.getConversationId());

            return R.ok(response);
        } else {
            return R.error(ApplicationCode.FAIL);
        }
    }


    /**
     * 批量发送消息
     * POST /social/chat/message/batch-send
     */
//    @PostMapping("/message/batch-send")
    public R<Map<String, Object>> batchSend(
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long currentUserId,
            @RequestBody List<MessageDto> messages) {
        try {
            Map<String, Object> result = new HashMap<>();

            if (messages == null || messages.isEmpty()) {
                return R.<Map<String, Object>>error().message("消息列表不能为空");
            }

            // 从header获取当前用户ID，覆盖所有消息的fromUserId
            for (MessageDto messageDto : messages) {
                messageDto.setFromUserId(currentUserId);
                RpcResult<Void> voidRpcResult = sensitiveWordsDubboService.checkSensitiveWords(messageDto.getContent());
                Assert.rpcResultOk(voidRpcResult);
            }

            // 收集所有接收方ID
            Set<Long> toUserIds = new HashSet<>();
            for (MessageDto messageDto : messages) {
                toUserIds.add(messageDto.getToUserId());
            }

            MessageDto firstMessage = messages.get(0);

            String sendResult = messageService.batchSendMessage(
                    currentUserId,
                    new ArrayList<>(toUserIds),
                    firstMessage
            );

            if (sendResult.equals("批量发送消息成功")) {
                result.put("success", true);
                result.put("message", "批量发送消息成功");
                result.put("sentToUserCount", toUserIds.size());
                return R.ok(result);
            } else {
                return R.<Map<String, Object>>error(ApplicationCode.FAIL).message(sendResult);
            }
        } catch (Exception e) {
            log.error("批量发送消息失败", e);
            return R.error(ApplicationCode.FAIL);
        }
    }

    /**
     * 撤回消息
     * POST /social/chat/message/{messageId}/recall
     */
    @PostMapping("/message/{messageId}/recall")
    @Operation(summary = "撤回消息", description = "撤回已发送的消息，仅发送者可以撤回")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "撤回成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<Boolean> recallMessage(
            @Parameter(description = "消息ID", required = true)
            @PathVariable Long messageId,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        try {
            if (userId == null) {
                log.error("请求头中缺少{}", HEADER_USER_ID);
                throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
            }
            String result = messageService.recallMessage(messageId, userId);
            if (result.equals("消息撤回成功")) {
                return R.ok(true);
            } else {
                log.info(result);
                return R.error(ApplicationCode.FAIL);
            }
        } catch (Exception e) {
            log.error("撤回消息失败", e);
            return R.error(ApplicationCode.FAIL);
        }
    }

    /**
     * 设置消息已读
     * POST /social/chat/message/{messageId}/read
     */
    @PostMapping("/conversation/{conversationId}/read")
    @Operation(summary = "标记会话已读", description = "标记指定会话的所有消息为已读状态")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "标记成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<Boolean> markConversationRead(
            @Parameter(description = "会话ID", required = true)
            @PathVariable String conversationId,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        try {
            if (userId == null) {
                log.error("请求头中缺少{}", HEADER_USER_ID);
                throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
            }
            Boolean result = conversationService.markConversationRead(Long.parseLong(conversationId), userId);
            if (result) {
                return R.ok(true);
            } else {
                return R.error(ApplicationCode.FAIL);
            }
        } catch (Exception e) {
            log.error("设置消息已读失败", e);
            return R.error(ApplicationCode.FAIL);
        }
    }

    /**
     * 删除消息
     * DELETE /social/chat/message/{messageId}
     */
    @DeleteMapping("/message/{messageId}")
    @Operation(summary = "删除消息", description = "删除指定消息（软删除，仅对当前用户隐藏）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<Boolean> deleteMessage(
            @Parameter(description = "消息ID", required = true)
            @PathVariable Long messageId,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        try {
            if (userId == null) {
                log.error("请求头中缺少{}", HEADER_USER_ID);
                throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
            }
            log.info("消息id{}", messageId);
            log.info("删除人id{}", userId);
            String result = messageService.deleteMessage(messageId, userId);
            if (result.equals(MessageResult.MESSAGE_DELETE_SUCCESS.getMessage())) {
                return R.ok(true);
            } else {
                log.info("删除消息失败");
                return R.error(ApplicationCode.FAIL);
            }
        } catch (Exception e) {
            log.error("删除消息失败", e);
            return R.error(ApplicationCode.FAIL);
        }
    }

    /**
     * 查询消息列表
     * GET /social/chat/message/list/{conversationId}
     */
    @GetMapping("/message/list/{conversationId}")
    @Operation(summary = "获取消息列表", description = "获取指定会话的消息列表，支持分页。仅会话参与者可以查看")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<MessageListVo> getMessageList(
            @Parameter(description = "会话ID", required = true)
            @PathVariable Long conversationId,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "页码（从1开始）", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
            if (userId == null) {
                log.error("请求头中缺少{}", HEADER_USER_ID);
                throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
            }
            Conversation conversation = conversationService.getConversationById(conversationId);
            if (conversation == null) {
                return R.<MessageListVo>error().message("会话不存在");
            }

            if (!conversation.getSenderUserId().equals(userId) && !conversation.getReceiveUserId().equals(userId)) {
                return R.<MessageListVo>error().message("无权访问此会话");
            }

            MessageListVo messageListByConversation = messageService.getMessageListByConversation(conversationId,
                    page,
                    pageSize,
                    userId);

            // 过滤撤回的消息

            return R.ok(messageListByConversation);
        } catch (Exception e) {
            log.error("查询消息列表失败", e);
            return R.error(ApplicationCode.FAIL);
        }
    }


    /**
     * 获取未读消息数量
     * GET /social/chat/unread/count
     */
    @GetMapping("/unread/count")
    @Operation(summary = "获取未读消息数量", description = "获取当前用户所有会话的未读消息总数和每个会话的未读数")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<Map<String, Object>> getUnreadCount(
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        try {
            Map<String, Object> result = new HashMap<>();

            // 获取用户所有会话，查询每个会话的未读数量
            PaginationResult<Conversation> conversationResult =
                    conversationService.getConversationList(userId, 1, 1000);

            List<Conversation> conversations = conversationResult.getList();
            List<Map<String, Object>> conversationUnreadList = new ArrayList<>();
            Long totalUnread = 0L;

            if (conversations != null) {
                for (Conversation conv : conversations) {
                    Map<String, Object> convInfo = new HashMap<>();
                    convInfo.put("conversationId", conv.getConversationId());

                    Long unreadCount = conv.getSenderUserId().equals(userId) ?
                            conv.getUnreadCountSender() : conv.getUnreadCountReceiver();

                    convInfo.put("unreadCount", unreadCount);
                    conversationUnreadList.add(convInfo);
                    totalUnread += unreadCount;
                }
            }

            result.put("totalUnread", totalUnread);
            result.put("conversations", conversationUnreadList);

            return R.ok(result);
        } catch (Exception e) {
            log.error("获取未读消息数量失败", e);
            return R.error(ApplicationCode.FAIL);
        }
    }
}
