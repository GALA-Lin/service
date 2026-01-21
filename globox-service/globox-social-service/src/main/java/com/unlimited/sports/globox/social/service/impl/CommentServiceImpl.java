package com.unlimited.sports.globox.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.GovernanceCode;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.SocialCode;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.NotificationSender;
import com.unlimited.sports.globox.common.enums.notification.NotificationEventEnum;
import com.unlimited.sports.globox.common.enums.notification.NotificationEntityTypeEnum;
import com.unlimited.sports.globox.dubbo.governance.SensitiveWordsDubboService;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoRequest;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoResponse;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.model.social.dto.CreateCommentRequest;
import com.unlimited.sports.globox.model.social.entity.SocialNote;
import com.unlimited.sports.globox.model.social.entity.SocialNoteComment;
import com.unlimited.sports.globox.model.social.entity.SocialNoteCommentLike;
import com.unlimited.sports.globox.model.social.vo.CommentItemVo;
import com.unlimited.sports.globox.model.social.vo.CursorPaginationResult;
import com.unlimited.sports.globox.social.mapper.SocialNoteCommentLikeMapper;
import com.unlimited.sports.globox.social.mapper.SocialNoteCommentMapper;
import com.unlimited.sports.globox.social.mapper.SocialNoteMapper;
import com.unlimited.sports.globox.social.service.CommentService;
import com.unlimited.sports.globox.social.util.CursorUtils;
import com.unlimited.sports.globox.social.util.SocialNotificationUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论服务实现
 */
@Service
@Slf4j
public class CommentServiceImpl implements CommentService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    @Autowired
    private SocialNoteCommentMapper commentMapper;

    @Autowired
    private SocialNoteCommentLikeMapper commentLikeMapper;

    @Autowired
    private SocialNoteMapper noteMapper;

    @DubboReference(group = "rpc")
    private UserDubboService userDubboService;

    @DubboReference(group = "rpc")
    private SensitiveWordsDubboService sensitiveWordsDubboService;

    @Autowired
    private NotificationSender notificationSender;

    @Autowired
    private SocialNotificationUtil socialNotificationUtil;

    @Override
    public R<CursorPaginationResult<CommentItemVo>> getCommentList(Long noteId, String cursor, Integer size, Long userId) {
        // 1. 参数校验
        if (noteId == null) {
            return R.error(SocialCode.COMMENT_NOT_FOUND);
        }

        // 2. 校验笔记存在且已发布
        SocialNote note = noteMapper.selectById(noteId);
        if (note == null || note.getStatus() != SocialNote.Status.PUBLISHED) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }

        // 3. 校验分页大小
        if (size == null || size <= 0) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (size > MAX_PAGE_SIZE) {
            throw new GloboxApplicationException(SocialCode.COMMENT_PAGE_SIZE_EXCEEDED);
        }

        // 4. 解析游标
        CursorUtils.CommentCursor cursorObj = null;
        LocalDateTime cursorTime = null;
        Long cursorCommentId = null;
        if (StringUtils.hasText(cursor)) {
            try {
                cursorObj = CursorUtils.parseCommentCursor(cursor);
                cursorTime = cursorObj.getCreatedAt();
                cursorCommentId = cursorObj.getCommentId();
            } catch (Exception e) {
                throw new GloboxApplicationException(SocialCode.COMMENT_CURSOR_INVALID);
            }
        }

        // 5. 查询评论列表（查询 size+1 条以判断是否有更多）
        List<SocialNoteComment> comments = commentMapper.selectCommentListWithCursor(
                noteId, cursorTime, cursorCommentId, size + 1);

        boolean hasMore = comments.size() > size;
        List<SocialNoteComment> resultComments = hasMore ? comments.subList(0, size) : comments;

        // 6. 转换为VO
        List<CommentItemVo> voList = convertToCommentItemVo(resultComments, userId);

        // 7. 构建下一个游标
        String nextCursor = null;
        if (hasMore && !resultComments.isEmpty()) {
            SocialNoteComment lastComment = resultComments.get(resultComments.size() - 1);
            nextCursor = CursorUtils.buildCommentCursor(lastComment.getCreatedAt(), lastComment.getCommentId());
        }

        // 8. 构建结果
        CursorPaginationResult<CommentItemVo> result = new CursorPaginationResult<>();
        result.setList(voList);
        result.setNextCursor(nextCursor);
        result.setHasMore(hasMore);

        return R.ok(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> createComment(Long userId, Long noteId, CreateCommentRequest request) {
        // 1. 参数校验
        if (userId == null || noteId == null) {
            return R.error(SocialCode.COMMENT_NOT_FOUND);
        }

        if (!StringUtils.hasText(request.getContent())) {
            throw new GloboxApplicationException(SocialCode.COMMENT_CONTENT_EMPTY);
        }

        if (request.getContent().length() > 300) {
            throw new GloboxApplicationException(SocialCode.COMMENT_CONTENT_EMPTY);
        }

        // 1.1 敏感词校验（仅评论内容）
        RpcResult<Void> rpcResult = sensitiveWordsDubboService.checkSensitiveWords(request.getContent());
        Assert.rpcResultOk(rpcResult);

        // 2. 校验笔记存在且已发布
        SocialNote note = noteMapper.selectById(noteId);
        if (note == null || note.getStatus() != SocialNote.Status.PUBLISHED) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }

        // 3. 校验评论是否关闭
        if (note.getAllowComment() == null || !note.getAllowComment()) {
            throw new GloboxApplicationException(SocialCode.COMMENT_CLOSED);
        }

        // 4. 如果是回复，校验父评论
        Long parentId = null;
        Long replyToUserId = null;
        if (request.getParentCommentId() != null) {
            SocialNoteComment parentComment = commentMapper.selectById(request.getParentCommentId());
            if (parentComment == null || parentComment.getStatus() != SocialNoteComment.Status.PUBLISHED) {
                throw new GloboxApplicationException(SocialCode.COMMENT_PARENT_INVALID);
            }
            if (!parentComment.getNoteId().equals(noteId)) {
                throw new GloboxApplicationException(SocialCode.COMMENT_PARENT_INVALID);
            }

            // 允许多级嵌套：直接挂到被回复评论
            parentId = parentComment.getCommentId();

            replyToUserId = request.getReplyToUserId() != null
                    ? request.getReplyToUserId()
                    : parentComment.getUserId();
        }

        // 5. 创建评论
        SocialNoteComment comment = SocialNoteComment.builder()
                .noteId(noteId)
                .userId(userId)
                .parentId(parentId)
                .replyToUserId(replyToUserId)
                .content(request.getContent())
                .likeCount(0)
                .status(SocialNoteComment.Status.PUBLISHED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        commentMapper.insert(comment);

        // 6. 原子更新笔记评论数
        noteMapper.incrementCommentCount(noteId);

        // 7. 发送通知（评论作者不是笔记作者时才发送，展示评论者信息）
        if (!note.getUserId().equals(userId)) {
            try {
                Map<String, Object> customData = new HashMap<>();
                customData.put("commentId", comment.getCommentId());
                customData.put("commentContent", comment.getContent());
                customData.put("noteId", noteId);
                notificationSender.sendNotification(
                        note.getUserId(),
                        NotificationEventEnum.SOCIAL_NOTE_COMMENTED,
                        noteId,
                        customData,
                        NotificationEntityTypeEnum.USER,
                        userId
                );
            } catch (Exception e) {
                log.warn("发送评论通知失败：noteId={}, commentId={}, error={}",
                        noteId, comment.getCommentId(), e.getMessage());
                // 通知失败不影响评论创建
            }
        }

        // 8. 如果是回复评论，发送通知给被回复的用户（且被回复的用户不是自己）
        if (replyToUserId != null && !replyToUserId.equals(userId)) {
            socialNotificationUtil.sendCommentRepliedNotification(
                    noteId, parentId, comment.getContent(), replyToUserId, userId
            );
        }

        log.info("评论创建成功：userId={}, noteId={}, commentId={}", userId, noteId, comment.getCommentId());
        return R.ok(comment.getCommentId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> deleteComment(Long userId, Long noteId, Long commentId) {
        // 1. 参数校验
        if (userId == null || noteId == null || commentId == null) {
            return R.error(SocialCode.COMMENT_NOT_FOUND);
        }

        // 2. 查询评论
        SocialNoteComment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != SocialNoteComment.Status.PUBLISHED) {
            return R.error(SocialCode.COMMENT_NOT_FOUND);
        }

        // 3. 校验评论属于该笔记
        if (!comment.getNoteId().equals(noteId)) {
            return R.error(SocialCode.COMMENT_NOT_FOUND);
        }

        // 4. 权限校验：评论作者或笔记作者可删除
        SocialNote note = noteMapper.selectById(noteId);
        if (note == null) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }
        if (!comment.getUserId().equals(userId) && !note.getUserId().equals(userId)) {
            throw new GloboxApplicationException(SocialCode.COMMENT_PERMISSION_DENIED);
        }

        // 5. 软删除评论
        comment.setStatus(SocialNoteComment.Status.DELETED);
        comment.setUpdatedAt(LocalDateTime.now());
        commentMapper.updateById(comment);

        // 6. 级联软删除所有回复（多级）
        List<SocialNoteComment> replies = collectAllReplies(commentId);
        int deleteCount = 1; // 包含当前评论
        if (!replies.isEmpty()) {
            for (SocialNoteComment reply : replies) {
                if (reply.getStatus() == SocialNoteComment.Status.PUBLISHED) {
                    deleteCount++;
                }
                reply.setStatus(SocialNoteComment.Status.DELETED);
                reply.setUpdatedAt(LocalDateTime.now());
                commentMapper.updateById(reply);
            }
        }
        // 8. 原子更新笔记评论数
        noteMapper.decrementCommentCount(noteId, deleteCount);

        log.info("评论删除成功：userId={}, noteId={}, commentId={}, 删除数量={}", 
                userId, noteId, commentId, deleteCount);
        return R.ok("删除成功");
    }

    private List<SocialNoteComment> collectAllReplies(Long parentCommentId) {
        List<SocialNoteComment> result = new ArrayList<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(parentCommentId);
        visited.add(parentCommentId);

        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            List<SocialNoteComment> replies = commentMapper.selectRepliesByParentId(currentId);
            if (replies == null || replies.isEmpty()) {
                continue;
            }
            for (SocialNoteComment reply : replies) {
                Long replyId = reply.getCommentId();
                if (replyId == null || !visited.add(replyId)) {
                    continue;
                }
                result.add(reply);
                queue.add(replyId);
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> likeComment(Long userId, Long noteId, Long commentId) {
        // 1. 参数校验
        if (userId == null || noteId == null || commentId == null) {
            return R.error(SocialCode.COMMENT_NOT_FOUND);
        }

        // 2. 查询评论是否存在且已发布
        SocialNoteComment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != SocialNoteComment.Status.PUBLISHED) {
            throw new GloboxApplicationException(SocialCode.COMMENT_NOT_FOUND);
        }

        // 3. 校验评论属于该笔记
        if (!comment.getNoteId().equals(noteId)) {
            throw new GloboxApplicationException(SocialCode.COMMENT_NOT_FOUND);
        }

        // 4. 检查是否已点赞（幂等性）
        LambdaQueryWrapper<SocialNoteCommentLike> query = new LambdaQueryWrapper<>();
        query.eq(SocialNoteCommentLike::getUserId, userId)
                .eq(SocialNoteCommentLike::getCommentId, commentId);
        SocialNoteCommentLike existingLike = commentLikeMapper.selectOne(query);

        if (existingLike == null) {
            // 5. 插入点赞记录
            SocialNoteCommentLike like = new SocialNoteCommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now());
            commentLikeMapper.insert(like);

            // 6. 原子更新评论点赞数
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentMapper.updateById(comment);

            // 7. 发送评论被点赞通知给评论作者
            Map<String, Object> customData = new HashMap<>();
            customData.put("noteId", noteId);
            customData.put("commentId", commentId);

            notificationSender.sendNotification(
                    comment.getUserId(),
                    NotificationEventEnum.SOCIAL_COMMENT_LIKED,
                    noteId,
                    customData,
                    NotificationEntityTypeEnum.USER,
                    userId
            );
        }

        return R.ok("点赞成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> unlikeComment(Long userId, Long noteId, Long commentId) {
        // 1. 参数校验
        if (userId == null || noteId == null || commentId == null) {
            return R.error(SocialCode.COMMENT_NOT_FOUND);
        }

        // 2. 查询评论是否存在且已发布
        SocialNoteComment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != SocialNoteComment.Status.PUBLISHED) {
            throw new GloboxApplicationException(SocialCode.COMMENT_NOT_FOUND);
        }

        // 3. 校验评论属于该笔记
        if (!comment.getNoteId().equals(noteId)) {
            throw new GloboxApplicationException(SocialCode.COMMENT_NOT_FOUND);
        }

        // 4. 查询点赞记录
        LambdaQueryWrapper<SocialNoteCommentLike> query = new LambdaQueryWrapper<>();
        query.eq(SocialNoteCommentLike::getUserId, userId)
                .eq(SocialNoteCommentLike::getCommentId, commentId);
        SocialNoteCommentLike existingLike = commentLikeMapper.selectOne(query);

        if (existingLike != null) {
            // 5. 删除点赞记录
            commentLikeMapper.deleteById(existingLike.getLikeId());

            // 6. 原子更新评论点赞数（确保不为负数）
            int newLikeCount = Math.max(comment.getLikeCount() - 1, 0);
            comment.setLikeCount(newLikeCount);
            commentMapper.updateById(comment);
        }

        return R.ok("取消点赞成功");
    }

    /**
     * 将评论列表转换为VO列表
     */
    private List<CommentItemVo> convertToCommentItemVo(List<SocialNoteComment> comments, Long userId) {
        if (CollectionUtils.isEmpty(comments)) {
            return new ArrayList<>();
        }

        // 1. 转换为VO
        List<CommentItemVo> voList = comments.stream()
                .map(comment -> {
                    CommentItemVo vo = new CommentItemVo();
                    BeanUtils.copyProperties(comment, vo);
                    return vo;
                })
                .collect(Collectors.toList());

        // 2. 批量查询用户点赞状态
        if (userId != null) {
            List<Long> commentIds = comments.stream()
                    .map(SocialNoteComment::getCommentId)
                    .collect(Collectors.toList());
            Set<Long> likedCommentIds = commentLikeMapper.selectLikedCommentIdsByUser(userId, commentIds);
            voList.forEach(vo -> vo.setLiked(likedCommentIds.contains(vo.getCommentId())));
        }

        // 3. 批量查询用户信息（评论者 + 被回复者）
        Set<Long> userIds = comments.stream()
                .map(SocialNoteComment::getUserId)
                .collect(Collectors.toSet());
        comments.stream()
                .filter(c -> c.getReplyToUserId() != null)
                .forEach(c -> userIds.add(c.getReplyToUserId()));

        Map<Long, UserInfoVo> userInfoMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            try {
                // 分批处理，每批最多50个
                List<Long> userIdList = new ArrayList<>(userIds);
                int batchSize = 50;
                for (int i = 0; i < userIdList.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, userIdList.size());
                    List<Long> batch = userIdList.subList(i, end);

                    BatchUserInfoRequest request = new BatchUserInfoRequest();
                    request.setUserIds(batch);
                    RpcResult<BatchUserInfoResponse> rpcResult = userDubboService.batchGetUserInfo(request);
                    Assert.rpcResultOk(rpcResult);
                    BatchUserInfoResponse response = rpcResult.getData();

                    if (response != null && response.getUsers() != null) {
                        response.getUsers().forEach(userInfo -> {
                            if (userInfo != null && userInfo.getUserId() != null) {
                                userInfoMap.put(userInfo.getUserId(), userInfo);
                            }
                        });
                    }
                }

                // 回填用户信息
                voList.forEach(vo -> {
                    // 评论者信息
                    UserInfoVo commenterInfo = userInfoMap.get(vo.getUserId());
                    if (commenterInfo != null) {
                        vo.setNickName(commenterInfo.getNickName());
                        vo.setAvatarUrl(commenterInfo.getAvatarUrl());
                    }

                    // 被回复者信息
                    if (vo.getReplyToUserId() != null) {
                        UserInfoVo replyToInfo = userInfoMap.get(vo.getReplyToUserId());
                        if (replyToInfo != null) {
                            vo.setReplyToUserName(replyToInfo.getNickName());
                        }
                    }
                });
            } catch (Exception e) {
                log.warn("批量获取评论用户信息失败：commentIds={}, error={}",
                        comments.stream().map(SocialNoteComment::getCommentId).collect(Collectors.toList()),
                        e.getMessage());
                // RPC异常不影响列表返回，只记录警告日志
            }
        }

        return voList;
    }
}

