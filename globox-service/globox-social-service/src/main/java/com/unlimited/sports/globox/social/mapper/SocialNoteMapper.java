package com.unlimited.sports.globox.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.social.entity.SocialNote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 笔记Mapper接口
 */
@Mapper
public interface SocialNoteMapper extends BaseMapper<SocialNote> {

    /**
     * 原子增加点赞数
     * 
     * @param noteId 笔记ID
     * @return 更新的行数
     */
    int incrementLikeCount(@Param("noteId") Long noteId);

    /**
     * 原子减少点赞数（确保不为负数）
     * 
     * @param noteId 笔记ID
     * @return 更新的行数
     */
    int decrementLikeCount(@Param("noteId") Long noteId);

    /**
     * 原子增加评论数
     * 
     * @param noteId 笔记ID
     * @return 更新的行数
     */
    int incrementCommentCount(@Param("noteId") Long noteId);

    /**
     * 原子减少评论数（确保不为负数）
     * 
     * @param noteId 笔记ID
     * @param count 减少的数量
     * @return 更新的行数
     */
    int decrementCommentCount(@Param("noteId") Long noteId, @Param("count") Integer count);

    /**
     * 批量更新笔记点赞计数（CASE WHEN 参数化）
     *
     * @param countChanges key=noteId, value=增量（正数加，负数减）
     */
    void batchUpdateLikeCounts(@Param("countChanges") Map<Long, Integer> countChanges);
}

