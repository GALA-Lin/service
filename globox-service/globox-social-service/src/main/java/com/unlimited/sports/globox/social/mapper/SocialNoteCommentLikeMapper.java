package com.unlimited.sports.globox.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.social.entity.SocialNoteCommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * 评论点赞Mapper接口
 */
@Mapper
public interface SocialNoteCommentLikeMapper extends BaseMapper<SocialNoteCommentLike> {

    /**
     * 批量查询用户对指定评论的点赞状态
     * 
     * @param userId 用户ID
     * @param commentIds 评论ID列表
     * @return 已点赞的评论ID集合
     */
    Set<Long> selectLikedCommentIdsByUser(
            @Param("userId") Long userId,
            @Param("commentIds") List<Long> commentIds
    );
}



