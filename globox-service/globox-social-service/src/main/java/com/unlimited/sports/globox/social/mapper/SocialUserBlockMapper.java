package com.unlimited.sports.globox.social.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.social.entity.SocialUserBlock;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 拉黑关系Mapper
 */
@Mapper
public interface SocialUserBlockMapper extends BaseMapper<SocialUserBlock> {

    default boolean existsBlock(Long userId, Long targetUserId) {
        LambdaQueryWrapper<SocialUserBlock> query = new LambdaQueryWrapper<>();
        query.eq(SocialUserBlock::getUserId, userId)
                .eq(SocialUserBlock::getBlockedUserId, targetUserId)
                .eq(SocialUserBlock::getDeleted, false)
                .last("LIMIT 1");
        return this.selectOne(query) != null;
    }

    default List<SocialUserBlock> selectBlocks(Long userId, List<Long> targetIds) {
        LambdaQueryWrapper<SocialUserBlock> query = new LambdaQueryWrapper<>();
        query.eq(SocialUserBlock::getUserId, userId)
                .eq(SocialUserBlock::getDeleted, false)
                .in(SocialUserBlock::getBlockedUserId, targetIds);
        return this.selectList(query);
    }
}





