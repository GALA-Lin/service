package com.unlimited.sports.globox.social.service;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.social.vo.FollowUserVo;
import com.unlimited.sports.globox.model.social.vo.UserRelationStatsVo;

/**
 * 关注/拉黑关系服务
 */
public interface SocialRelationService {

    R<String> follow(Long userId, Long targetUserId);

    R<String> unfollow(Long userId, Long targetUserId);

    R<String> block(Long userId, Long targetUserId);

    R<String> unblock(Long userId, Long targetUserId);

    R<PaginationResult<FollowUserVo>> getFollowing(Long viewerId, Long targetUserId, Integer page, Integer pageSize, String keyword);

    R<PaginationResult<FollowUserVo>> getFans(Long viewerId, Long targetUserId, Integer page, Integer pageSize, String keyword);

    R<PaginationResult<FollowUserVo>> getMutual(Long viewerId, Long targetUserId, Integer page, Integer pageSize, String keyword);

    R<UserRelationStatsVo> getUserStats(Long targetUserId);

    /**
     * 检查是否存在任意方向的拉黑关系
     */
    boolean isBlocked(Long viewerId, Long targetUserId);

    /**
     * 获取我拉黑的用户列表
     */
    R<PaginationResult<com.unlimited.sports.globox.model.social.vo.BlockUserVo>> getBlockedUsers(Long userId, Integer page, Integer pageSize, String keyword);
}





