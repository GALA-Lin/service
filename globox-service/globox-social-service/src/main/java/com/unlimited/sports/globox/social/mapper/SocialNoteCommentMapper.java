package com.unlimited.sports.globox.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.social.entity.SocialNoteComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论Mapper接口
 */
@Mapper
public interface SocialNoteCommentMapper extends BaseMapper<SocialNoteComment> {

    /**
     * 使用游标分页查询评论列表
     * 
     * @param noteId 笔记ID
     * @param cursorTime 游标时间
     * @param cursorCommentId 游标评论ID
     * @param limit 限制数量
     * @return 评论列表
     */
    List<SocialNoteComment> selectCommentListWithCursor(
            @Param("noteId") Long noteId,
            @Param("cursorTime") LocalDateTime cursorTime,
            @Param("cursorCommentId") Long cursorCommentId,
            @Param("limit") Integer limit
    );

    /**
     * 统计已发布的评论数量
     * 
     * @param noteId 笔记ID
     * @return 评论数量
     */
    int countPublishedCommentsByNoteId(@Param("noteId") Long noteId);

    /**
     * 查询指定父评论的所有回复
     * 
     * @param parentId 父评论ID
     * @return 回复列表
     */
    List<SocialNoteComment> selectRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * 原子递增评论点赞数
     */
    int incrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 原子递减评论点赞数（不会小于0）
     */
    int decrementLikeCount(@Param("commentId") Long commentId);
}



