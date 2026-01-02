package com.unlimited.sports.globox.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.social.entity.SocialNote;
import com.unlimited.sports.globox.model.social.entity.SocialNoteLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 笔记点赞Mapper
 */
@Mapper
public interface SocialNoteLikeMapper extends BaseMapper<SocialNoteLike> {

    /**
     * 使用 JOIN 查询用户点赞的笔记列表（只返回 PUBLISHED 状态的笔记）
     * 
     * @param userId 用户ID
     * @param cursorTime 游标时间（可选，用于分页）
     * @param cursorLikeId 游标点赞ID（可选，用于分页）
     * @param limit 查询数量限制
     * @return 笔记列表
     */
    List<SocialNote> selectLikedNotesWithJoin(@Param("userId") Long userId,
                                              @Param("cursorTime") LocalDateTime cursorTime,
                                              @Param("cursorLikeId") Long cursorLikeId,
                                              @Param("limit") Integer limit);

    /**
     * 批量查询用户对指定笔记的点赞状态
     * 
     * @param userId 用户ID
     * @param noteIds 笔记ID列表
     * @return 已点赞的笔记ID集合
     */
    Set<Long> selectLikedNoteIdsByUser(@Param("userId") Long userId,
                                       @Param("noteIds") List<Long> noteIds);
}

