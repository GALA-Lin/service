package com.unlimited.sports.globox.social.service;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.social.dto.CreateCommentRequest;
import com.unlimited.sports.globox.model.social.vo.CommentItemVo;
import com.unlimited.sports.globox.model.social.vo.CursorPaginationResult;

/**
 * 评论服务接口
 */
public interface CommentService {

    /**
     * 获取评论列表（游标分页）
     *
     * @param noteId 笔记ID
     * @param cursor 游标（可选）
     * @param size   每页数量
     * @param userId 用户ID（可选，用于查询 liked 状态）
     * @return 游标分页结果
     */
    R<CursorPaginationResult<CommentItemVo>> getCommentList(Long noteId, String cursor, Integer size, Long userId);

    /**
     * 发布评论
     *
     * @param userId  用户ID
     * @param noteId  笔记ID
     * @param request 创建评论请求
     * @return 新创建的评论ID
     */
    R<Long> createComment(Long userId, Long noteId, CreateCommentRequest request);

    /**
     * 删除评论
     *
     * @param userId   用户ID
     * @param noteId   笔记ID
     * @param commentId 评论ID
     * @return 成功提示
     */
    R<String> deleteComment(Long userId, Long noteId, Long commentId);

    /**
     * 点赞评论
     *
     * @param userId   用户ID
     * @param noteId   笔记ID
     * @param commentId 评论ID
     * @return 成功提示
     */
    R<String> likeComment(Long userId, Long noteId, Long commentId);

    /**
     * 取消点赞评论
     *
     * @param userId   用户ID
     * @param noteId   笔记ID
     * @param commentId 评论ID
     * @return 成功提示
     */
    R<String> unlikeComment(Long userId, Long noteId, Long commentId);
}



