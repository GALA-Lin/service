package com.unlimited.sports.globox.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.model.social.entity.SocialUserFollow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 关注关系Mapper
 */
@Mapper
public interface SocialUserFollowMapper extends BaseMapper<SocialUserFollow> {

    /**
     * 分页查询关注列表（按关注时间倒序）
     */
    default Page<SocialUserFollow> selectFollowingPage(Long userId, Page<SocialUserFollow> page) {
        LambdaQueryWrapper<SocialUserFollow> query = new LambdaQueryWrapper<>();
        query.eq(SocialUserFollow::getUserId, userId)
                .orderByDesc(SocialUserFollow::getCreatedAt);
        return this.selectPage(page, query);
    }

    /**
     * 分页查询粉丝列表（按关注时间倒序）
     */
    default Page<SocialUserFollow> selectFansPage(Long userId, Page<SocialUserFollow> page) {
        LambdaQueryWrapper<SocialUserFollow> query = new LambdaQueryWrapper<>();
        query.eq(SocialUserFollow::getFollowUserId, userId)
                .orderByDesc(SocialUserFollow::getCreatedAt);
        return this.selectPage(page, query);
    }

    /**
     * 查询互相关注的 followUserId 列表（基于我关注的用户，过滤对方是否关注我）
     */
    default List<SocialUserFollow> selectMutualBase(Long userId) {
        LambdaQueryWrapper<SocialUserFollow> query = new LambdaQueryWrapper<>();
        query.eq(SocialUserFollow::getUserId, userId);
        return this.selectList(query);
    }
}




