package com.unlimited.sports.globox.social.util;

import com.unlimited.sports.globox.common.enums.notification.NotificationEventEnum;
import com.unlimited.sports.globox.common.enums.notification.NotificationEntityTypeEnum;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.NotificationSender;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.model.social.entity.RallyPosts;
import com.unlimited.sports.globox.model.social.entity.RallyPostsStatusEnum;
import com.unlimited.sports.globox.social.constants.NoteCommentConstants;
import com.unlimited.sports.globox.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.unlimited.sports.globox.social.consts.SocialRedisKeyConstants.NOTE_LIKE_NOTIFY_DEDUP_PREFIX;

/**
 * 社交模块通知工具类
 * 负责发送所有社交相关的通知消息（仅发送，不做查询）
 * 包括消息通知、点赞通知、约球通知、关注通知等
 */
@Slf4j
@Component
public class SocialNotificationUtil {

    @Autowired
    private NotificationSender notificationSender;

    @Autowired
    private RedisService redisService;

    @DubboReference(group = "rpc")
    private UserDubboService userDubboService;

    /**
     * 发送新消息通知给接收人（展示发送者信息）
     *
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @param toUserId 接收人ID
     * @param fromUserId 发送人ID
     * @param messageContent 消息内容
     */
    public void sendChatMessageNotification(Long conversationId, Long messageId, Long toUserId, Long fromUserId, String messageContent) {
        try {
            // 获取发送者信息（用于推送到手机端显示昵称）
            RpcResult<UserInfoVo> senderResult = userDubboService.getUserInfo(fromUserId);
            Assert.rpcResultOk(senderResult);


            UserInfoVo senderInfo = senderResult.getData();
            Map<String, Object> customData = new HashMap<>();
            customData.put("conversationId", conversationId);
            customData.put("senderId", fromUserId);
            customData.put("senderName", senderInfo != null ? senderInfo.getNickName() : "");
            customData.put("messageContent", truncateContent(messageContent, NoteCommentConstants.COMMENT_REPLY_MAX_LENGTH));

            notificationSender.sendNotification(
                    toUserId,
                    NotificationEventEnum.SOCIAL_CHAT_MESSAGE_RECEIVED,
                    conversationId,
                    customData,
                    NotificationEntityTypeEnum.USER,
                    fromUserId
            );
            log.info("[消息通知] 发送成功 - conversationId={}, toUserId={}, fromUserId={}",
                    conversationId, toUserId, fromUserId);
        } catch (Exception e) {
            log.error("[消息通知] 发送失败 - conversationId={}, toUserId={}, fromUserId={}, error={}",
                    conversationId, toUserId, fromUserId, e.getMessage());
        }
    }

    /**
     * 发送帖子被点赞通知给作者（展示点赞人信息）
     *
     * @param noteId 笔记ID
     * @param likerId 点赞人ID
     * @param noteTitle 笔记标题
     * @param noteAuthorId 笔记作者ID
     */
    public void sendNoteLikedNotification(Long noteId, Long likerId, String noteTitle, Long noteAuthorId) {
        try {
            String dedupKey = NOTE_LIKE_NOTIFY_DEDUP_PREFIX + noteId + ":" + noteAuthorId + ":" + likerId;
            Boolean dedupSet = redisService.setCacheObjectIfAbsent(dedupKey, "1", 1L, TimeUnit.DAYS);
            if (Boolean.FALSE.equals(dedupSet)) {
                log.info("[帖子点赞通知] 去重命中，跳过发送 - noteId={}, noteAuthorId={}, likerId={}",
                        noteId, noteAuthorId, likerId);
                return;
            }

            // 获取点赞人的用户信息（用于推送到手机端显示昵称）
            RpcResult<UserInfoVo> likerResult = userDubboService.getUserInfo(likerId);
            if (!likerResult.isSuccess()) {
                log.error("[帖子点赞通知] 获取点赞人信息失败：likerId={}", likerId);
                return;
            }

            UserInfoVo likerInfo = likerResult.getData();
            Map<String, Object> customData = new HashMap<>();
            customData.put("noteId", noteId);
            customData.put("noteTitle", noteTitle);
            customData.put("likerId", likerId);
            customData.put("likerName", likerInfo != null ? likerInfo.getNickName() : null);

            notificationSender.sendNotification(
                    noteAuthorId,
                    NotificationEventEnum.SOCIAL_NOTE_LIKED,
                    noteId,
                    customData,
                    NotificationEntityTypeEnum.USER,
                    likerId
            );
            log.info("[帖子点赞通知] 发送成功 - noteId={}, noteAuthorId={}, likerId={}",
                    noteId, noteAuthorId, likerId);
        } catch (Exception e) {
            log.error("[帖子点赞通知] 发送失败 - noteId={}, likerId={}, error={}",
                    noteId, likerId, e.getMessage());
        }
    }

    /**
     * 发送被关注通知给被关注者（展示关注者信息）
     *
     * @param followedUserId 被关注的用户ID
     * @param followerId 关注者ID
     */
    public void sendFollowNotification(Long followedUserId, Long followerId) {
        try {
            // 获取关注者的用户信息（用于推送到手机端显示昵称）
            RpcResult<UserInfoVo> followerResult = userDubboService.getUserInfo(followerId);
            if (!followerResult.isSuccess()) {
                log.error("[关注通知] 获取关注者信息失败：followerId={}", followerId);
                return;
            }

            UserInfoVo followerInfo = followerResult.getData();
            Map<String, Object> customData = new HashMap<>();
            customData.put("followerId", followerId);
            customData.put("followerName", followerInfo != null ? followerInfo.getNickName() : null);

            notificationSender.sendNotification(
                    followedUserId,
                    NotificationEventEnum.SOCIAL_FOLLOWED,
                    followerId,
                    customData,
                    NotificationEntityTypeEnum.USER,
                    followerId
            );
            log.info("[关注通知] 发送成功 - followedUserId={}, followerId={}",
                    followedUserId, followerId);
        } catch (Exception e) {
            log.error("[关注通知] 发送失败 - followedUserId={}, followerId={}, error={}",
                    followedUserId, followerId, e.getMessage());
        }
    }

    /**
     * 发送约球参与申请通知给发起人（展示申请人信息）
     *
     * @param rallyId 约球ID
     * @param applicantId 申请人ID
     * @param rallyPosts 约球信息
     */
    public void sendRallyApplicationNotification(Long rallyId, Long applicantId, RallyPosts rallyPosts) {
        try {
            // 获取申请人信息（用于推送到手机端显示昵称）
            RpcResult<UserInfoVo> applicantResult = userDubboService.getUserInfo(applicantId);
            if (!applicantResult.isSuccess()) {
                log.error("[约球申请通知] 获取申请人信息失败：applicantId={}", applicantId);
                return;
            }

            UserInfoVo applicantInfo = applicantResult.getData();
            Map<String, Object> customData = new HashMap<>();
            customData.put("rallyId", rallyId);
            customData.put("rallyTitle", rallyPosts.getRallyTitle());
            customData.put("rallyDate", rallyPosts.getRallyEventDate());
            customData.put("rallyTime", rallyPosts.getRallyStartTime() + "-" + rallyPosts.getRallyEndTime());
            customData.put("rallyStatus", rallyPosts.getRallyStatus());
            customData.put("rallyStatusDesc", RallyPostsStatusEnum.getDescriptionByCode(rallyPosts.getRallyStatus()));
            customData.put("applicantId", applicantId);
            customData.put("applicantName", applicantInfo != null ? applicantInfo.getNickName() : null);

            notificationSender.sendNotification(
                    rallyPosts.getInitiatorId(),
                    NotificationEventEnum.RALLY_PARTICIPANT_APPLICATION,
                    rallyId,
                    customData,
                    NotificationEntityTypeEnum.USER,
                    applicantId
            );
            log.info("[约球申请通知] 发送成功 - rallyId={}, initiatorId={}, applicantId={}",
                    rallyId, rallyPosts.getInitiatorId(), applicantId);
        } catch (Exception e) {
            log.error("[约球申请通知] 发送失败 - rallyId={}, applicantId={}, error={}",
                    rallyId, applicantId, e.getMessage());
        }
    }

    /**
     * 发送申请被接受通知给申请人
     *
     * @param rallyId 约球ID
     * @param applicantId 申请人ID
     * @param rallyPosts 约球信息
     */
    public void sendRallyApplicationAcceptedNotification(Long rallyId, Long applicantId, RallyPosts rallyPosts) {
        try {
            Map<String, Object> customData = new HashMap<>();
            customData.put("rallyId", rallyId);
            customData.put("rallyTitle", rallyPosts.getRallyTitle());
            customData.put("rallyDate", rallyPosts.getRallyEventDate());
            customData.put("rallyStatus", rallyPosts.getRallyStatus());
            customData.put("rallyStatusDesc", RallyPostsStatusEnum.getDescriptionByCode(rallyPosts.getRallyStatus()));

            notificationSender.sendNotification(
                    applicantId,
                    NotificationEventEnum.RALLY_APPLICATION_ACCEPTED,
                    rallyId,
                    customData
            );
            log.info("[约球申请接受通知] 发送成功 - rallyId={}, applicantId={}",
                    rallyId, applicantId);
        } catch (Exception e) {
            log.error("[约球申请接受通知] 发送失败 - rallyId={}, applicantId={}, error={}",
                    rallyId, applicantId, e.getMessage());
        }
    }

    /**
     * 发送人数已满通知给发起人
     *
     * @param rallyId 约球ID
     * @param rallyPosts 约球信息
     * @param initiatorId 发起人ID
     */
    public void sendRallyFullNotification(Long rallyId, RallyPosts rallyPosts, Long initiatorId) {
        try {
            Map<String, Object> customData = new HashMap<>();
            customData.put("rallyId", rallyId);
            customData.put("rallyTitle", rallyPosts.getRallyTitle());
            customData.put("currentCount", rallyPosts.getRallyTotalPeople() - rallyPosts.getRallyRemainingPeople());
            customData.put("maxCount", rallyPosts.getRallyTotalPeople());
            customData.put("rallyStatus", rallyPosts.getRallyStatus());
            customData.put("rallyStatusDesc", RallyPostsStatusEnum.getDescriptionByCode(rallyPosts.getRallyStatus()));

            notificationSender.sendNotification(
                    initiatorId,
                    NotificationEventEnum.RALLY_PARTICIPANTS_FULL,
                    rallyId,
                    customData
            );
            log.info("[约球人数已满通知] 发送成功 - rallyId={}, initiatorId={}",
                    rallyId, initiatorId);
        } catch (Exception e) {
            log.error("[约球人数已满通知] 发送失败 - rallyId={}, initiatorId={}, error={}",
                    rallyId, initiatorId, e.getMessage());
        }
    }

    /**
     * 发送人数已满且申请被接受通知给申请人
     * 用于人数已满时接受最后一个申请的场景
     *
     * @param rallyId 约球ID
     * @param applicantId 申请人ID
     * @param rallyPosts 约球信息
     */
    public void sendRallyParticipantsFullAcceptedNotification(Long rallyId, Long applicantId, RallyPosts rallyPosts) {
        try {
            Map<String, Object> customData = new HashMap<>();
            customData.put("rallyId", rallyId);
            customData.put("rallyTitle", rallyPosts.getRallyTitle());
            customData.put("rallyDate", rallyPosts.getRallyEventDate());
            customData.put("rallyStatus", rallyPosts.getRallyStatus());
            customData.put("rallyStatusDesc", RallyPostsStatusEnum.getDescriptionByCode(rallyPosts.getRallyStatus()));
            customData.put("acceptedAt", LocalDateTime.now().toString());

            notificationSender.sendNotification(
                    applicantId,
                    NotificationEventEnum.RALLY_PARTICIPANTS_FULL_ACCEPTED,
                    rallyId,
                    customData
            );
            log.info("[约球人数已满接受通知] 发送成功 - rallyId={}, applicantId={}",
                    rallyId, applicantId);
        } catch (Exception e) {
            log.error("[约球人数已满接受通知] 发送失败 - rallyId={}, applicantId={}, error={}",
                    rallyId, applicantId, e.getMessage());
        }
    }

    /**
     * 发送人数已满且申请被拒绝通知给申请人
     * 用于人数已满时拒绝其他申请的场景
     *
     * @param rallyId 约球ID
     * @param applicantId 申请人ID
     * @param rallyPosts 约球信息
     */
    public void sendRallyParticipantsFullRejectedNotification(Long rallyId, Long applicantId, RallyPosts rallyPosts) {
        try {
            Map<String, Object> customData = new HashMap<>();
            customData.put("rallyId", rallyId);
            customData.put("rallyTitle", rallyPosts.getRallyTitle());
            customData.put("rallyDate", rallyPosts.getRallyEventDate());
            customData.put("rallyStatus", rallyPosts.getRallyStatus());
            customData.put("rallyStatusDesc", RallyPostsStatusEnum.getDescriptionByCode(rallyPosts.getRallyStatus()));
            customData.put("rejectedAt", LocalDateTime.now().toString());

            notificationSender.sendNotification(
                    applicantId,
                    NotificationEventEnum.RALLY_PARTICIPANTS_FULL_REJECTED,
                    rallyId,
                    customData
            );
            log.info("[约球人数已满拒绝通知] 发送成功 - rallyId={}, applicantId={}",
                    rallyId, applicantId);
        } catch (Exception e) {
            log.error("[约球人数已满拒绝通知] 发送失败 - rallyId={}, applicantId={}, error={}",
                    rallyId, applicantId, e.getMessage());
        }
    }

    /**
     * 发送参与者退出通知给发起人（展示退出的参与者信息）
     *
     * @param rallyId 约球ID
     * @param participantId 退出的参与者ID
     * @param rallyPosts 约球信息
     */
    public void sendRallyQuitNotification(Long rallyId, Long participantId, RallyPosts rallyPosts) {
        try {
            Map<String, Object> customData = new HashMap<>();
            customData.put("rallyId", rallyId);
            customData.put("rallyTitle", rallyPosts.getRallyTitle());
            customData.put("rallyStatus", rallyPosts.getRallyStatus());
            customData.put("rallyStatusDesc", RallyPostsStatusEnum.getDescriptionByCode(rallyPosts.getRallyStatus()));
            customData.put("participantId", participantId);

            notificationSender.sendNotification(
                    rallyPosts.getInitiatorId(),
                    NotificationEventEnum.RALLY_PARTICIPANT_QUIT,
                    rallyId,
                    customData,
                    NotificationEntityTypeEnum.USER,
                    participantId
            );
            log.info("[约球退出通知] 发送成功 - rallyId={}, participantId={}, initiatorId={}",
                    rallyId, participantId, rallyPosts.getInitiatorId());
        } catch (Exception e) {
            log.error("[约球退出通知] 发送失败 - rallyId={}, participantId={}, error={}",
                    rallyId, participantId, e.getMessage());
        }
    }

    /**
     * 发送约球已取消通知给参与者
     *
     * @param rallyId 约球ID
     * @param participantId 参与者ID
     * @param rallyPosts 约球信息
     */
    public void sendRallyCancelledNotification(Long rallyId, Long participantId, RallyPosts rallyPosts) {
        try {
            Map<String, Object> customData = new HashMap<>();
            customData.put("rallyId", rallyId);
            customData.put("rallyTitle", rallyPosts.getRallyTitle());
            customData.put("rallyStatus", rallyPosts.getRallyStatus());
            customData.put("rallyStatusDesc", RallyPostsStatusEnum.getDescriptionByCode(rallyPosts.getRallyStatus()));

            notificationSender.sendNotification(
                    participantId,
                    NotificationEventEnum.RALLY_CANCELLED,
                    rallyId,
                    customData
            );
            log.info("[约球取消通知] 发送成功 - rallyId={}, participantId={}, rallyTitle={}",
                    rallyId, participantId, rallyPosts.getRallyTitle());
        } catch (Exception e) {
            log.error("[约球取消通知] 发送失败 - rallyId={}, participantId={}, error={}",
                    rallyId, participantId, e.getMessage());
        }
    }

    /**
     * 发送评论被回复通知给被回复的评论作者（展示回复人信息）
     *
     * @param noteId 笔记ID
     * @param parentCommentId 被回复的评论ID
     * @param commentContent 回复的评论内容
     * @param replyToUserId 被回复的用户ID
     * @param repliedByUserId 回复的用户ID
     */
    public void sendCommentRepliedNotification(Long noteId, Long parentCommentId, String commentContent, Long replyToUserId, Long repliedByUserId) {
        try {
            // 获取回复人的用户信息（用于推送到手机端显示昵称）
            RpcResult<UserInfoVo> replierResult = userDubboService.getUserInfo(repliedByUserId);
            if (!replierResult.isSuccess()) {
                log.error("[评论回复通知] 获取回复人信息失败：repliedByUserId={}", repliedByUserId);
                return;
            }

            UserInfoVo replierInfo = replierResult.getData();
            Map<String, Object> customData = new HashMap<>();
            customData.put("noteId", noteId);
            customData.put("parentCommentId", parentCommentId);
            customData.put("commentContent", truncateContent(commentContent, NoteCommentConstants.COMMENT_REPLY_MAX_LENGTH));
            customData.put("repliedByUserId", repliedByUserId);
            customData.put("replierName", replierInfo != null ? replierInfo.getNickName() : null);

            notificationSender.sendNotification(
                    replyToUserId,
                    NotificationEventEnum.SOCIAL_COMMENT_REPLIED,
                    noteId,
                    customData,
                    NotificationEntityTypeEnum.USER,
                    repliedByUserId
            );
            log.info("[评论回复通知] 发送成功 - noteId={}, parentCommentId={}, replyToUserId={}, repliedByUserId={}",
                    noteId, parentCommentId, replyToUserId, repliedByUserId);
        } catch (Exception e) {
            log.error("[评论回复通知] 发送失败 - noteId={}, replyToUserId={}, repliedByUserId={}, error={}",
                    noteId, replyToUserId, repliedByUserId, e.getMessage());
        }
    }

    /**
     * 截断消息内容到指定长度
     *
     * @param content 原始内容
     * @param maxLength 最大长度
     * @return 截断后的内容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
