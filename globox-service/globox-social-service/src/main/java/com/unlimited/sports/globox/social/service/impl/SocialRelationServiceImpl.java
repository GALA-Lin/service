package com.unlimited.sports.globox.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.SocialCode;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoRequest;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoResponse;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.model.social.entity.SocialNote;
import com.unlimited.sports.globox.model.social.entity.SocialUserBlock;
import com.unlimited.sports.globox.model.social.entity.SocialUserFollow;
import com.unlimited.sports.globox.model.social.vo.FollowUserVo;
import com.unlimited.sports.globox.model.social.vo.UserRelationStatsVo;
import com.unlimited.sports.globox.social.mapper.SocialNoteMapper;
import com.unlimited.sports.globox.social.mapper.SocialUserBlockMapper;
import com.unlimited.sports.globox.social.mapper.SocialUserFollowMapper;
import com.unlimited.sports.globox.social.service.SocialRelationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SocialRelationServiceImpl implements SocialRelationService {

    @Autowired
    private SocialUserFollowMapper socialUserFollowMapper;

    @Autowired
    private SocialUserBlockMapper socialUserBlockMapper;

    @Autowired
    private SocialNoteMapper socialNoteMapper;

    @DubboReference(group = "rpc")
    private UserDubboService userDubboService;

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> follow(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }
        if (userId.equals(targetUserId)) {
            throw new GloboxApplicationException(SocialCode.FOLLOW_SELF_NOT_ALLOWED);
        }
        // 拉黑校验
        if (isBlocked(userId, targetUserId)) {
            throw new GloboxApplicationException(SocialCode.FOLLOW_DISABLED_BY_BLOCK);
        }

        // 幂等：已关注直接返回成功
        LambdaQueryWrapper<SocialUserFollow> existQuery = new LambdaQueryWrapper<>();
        existQuery.eq(SocialUserFollow::getUserId, userId)
                .eq(SocialUserFollow::getFollowUserId, targetUserId)
                .last("LIMIT 1");
        SocialUserFollow exist = socialUserFollowMapper.selectOne(existQuery);
        if (exist != null) {
            return R.ok("关注成功");
        }

        SocialUserFollow follow = SocialUserFollow.builder()
                .userId(userId)
                .followUserId(targetUserId)
                .createdAt(LocalDateTime.now())
                .build();
        socialUserFollowMapper.insert(follow);
        return R.ok("关注成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> unfollow(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }
        LambdaQueryWrapper<SocialUserFollow> query = new LambdaQueryWrapper<>();
        query.eq(SocialUserFollow::getUserId, userId)
                .eq(SocialUserFollow::getFollowUserId, targetUserId);
        socialUserFollowMapper.delete(query);
        return R.ok("取消关注成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> block(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }
        if (userId.equals(targetUserId)) {
            throw new GloboxApplicationException(SocialCode.FOLLOW_SELF_NOT_ALLOWED);
        }
        // 已拉黑直接返回
        if (socialUserBlockMapper.existsBlock(userId, targetUserId)) {
            return R.ok("拉黑成功");
        }

        SocialUserBlock block = SocialUserBlock.builder()
                .userId(userId)
                .blockedUserId(targetUserId)
                .createdAt(LocalDateTime.now())
                .build();
        socialUserBlockMapper.insert(block);

        // 删除双向关注
        LambdaQueryWrapper<SocialUserFollow> del1 = new LambdaQueryWrapper<>();
        del1.eq(SocialUserFollow::getUserId, userId)
                .eq(SocialUserFollow::getFollowUserId, targetUserId);
        socialUserFollowMapper.delete(del1);

        LambdaQueryWrapper<SocialUserFollow> del2 = new LambdaQueryWrapper<>();
        del2.eq(SocialUserFollow::getUserId, targetUserId)
                .eq(SocialUserFollow::getFollowUserId, userId);
        socialUserFollowMapper.delete(del2);

        return R.ok("拉黑成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> unblock(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }
        LambdaQueryWrapper<SocialUserBlock> query = new LambdaQueryWrapper<>();
        query.eq(SocialUserBlock::getUserId, userId)
                .eq(SocialUserBlock::getBlockedUserId, targetUserId);
        socialUserBlockMapper.delete(query);
        return R.ok("取消拉黑成功");
    }

    @Override
    public R<PaginationResult<FollowUserVo>> getFollowing(Long userId, Integer page, Integer pageSize, String keyword) {
        Page<SocialUserFollow> pageParam = buildPage(userId, page, pageSize);
        Page<SocialUserFollow> pageResult = socialUserFollowMapper.selectFollowingPage(userId, pageParam);
        return R.ok(buildFollowVoPage(userId, pageResult.getRecords(), pageResult.getTotal(), keyword, true));
    }

    @Override
    public R<PaginationResult<FollowUserVo>> getFans(Long userId, Integer page, Integer pageSize, String keyword) {
        Page<SocialUserFollow> pageParam = buildPage(userId, page, pageSize);
        Page<SocialUserFollow> pageResult = socialUserFollowMapper.selectFansPage(userId, pageParam);
        return R.ok(buildFollowVoPage(userId, pageResult.getRecords(), pageResult.getTotal(), keyword, false));
    }

    @Override
    public R<PaginationResult<FollowUserVo>> getMutual(Long userId, Integer page, Integer pageSize, String keyword) {
        // 基于关注列表筛选互关
        Page<SocialUserFollow> pageParam = buildPage(userId, page, pageSize);
        Page<SocialUserFollow> followingPage = socialUserFollowMapper.selectFollowingPage(userId, pageParam);
        List<SocialUserFollow> following = followingPage.getRecords();
        if (CollectionUtils.isEmpty(following)) {
            PaginationResult<FollowUserVo> empty = PaginationResult.build(
                    new ArrayList<>(),
                    followingPage.getTotal(),
                    (int) pageParam.getCurrent(),
                    (int) pageParam.getSize());
            return R.ok(empty);
        }
        List<Long> targetIds = following.stream().map(SocialUserFollow::getFollowUserId).collect(Collectors.toList());
        LambdaQueryWrapper<SocialUserFollow> reverseQuery = new LambdaQueryWrapper<>();
        reverseQuery.eq(SocialUserFollow::getFollowUserId, userId)
                .in(SocialUserFollow::getUserId, targetIds);
        List<SocialUserFollow> reverse = socialUserFollowMapper.selectList(reverseQuery);
        Set<Long> mutualSet = reverse.stream().map(SocialUserFollow::getUserId).collect(Collectors.toSet());
        List<SocialUserFollow> mutualList = following.stream()
                .filter(f -> mutualSet.contains(f.getFollowUserId()))
                .collect(Collectors.toList());
        // total 按当前页互关数量，简化处理
        return R.ok(buildFollowVoPage(userId, mutualList, mutualList.size(), keyword, true));
    }

    @Override
    public R<UserRelationStatsVo> getUserStats(Long targetUserId) {
        if (targetUserId == null) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }
        // 关注数
        LambdaQueryWrapper<SocialUserFollow> followCountQuery = new LambdaQueryWrapper<>();
        followCountQuery.eq(SocialUserFollow::getUserId, targetUserId);
        long followCount = socialUserFollowMapper.selectCount(followCountQuery);

        // 粉丝数
        LambdaQueryWrapper<SocialUserFollow> fansCountQuery = new LambdaQueryWrapper<>();
        fansCountQuery.eq(SocialUserFollow::getFollowUserId, targetUserId);
        long fansCount = socialUserFollowMapper.selectCount(fansCountQuery);

        // 获赞数（已发布笔记）
        LambdaQueryWrapper<SocialNote> likeSumQuery = new LambdaQueryWrapper<>();
        likeSumQuery.eq(SocialNote::getUserId, targetUserId)
                .eq(SocialNote::getStatus, SocialNote.Status.PUBLISHED)
                .select(SocialNote::getLikeCount);
        List<SocialNote> notes = socialNoteMapper.selectList(likeSumQuery);
        long likeCount = notes.stream().mapToLong(n -> n.getLikeCount() == null ? 0 : n.getLikeCount()).sum();

        UserRelationStatsVo vo = new UserRelationStatsVo();
        vo.setFollowCount(followCount);
        vo.setFansCount(fansCount);
        vo.setLikeCount(likeCount);
        return R.ok(vo);
    }

    @Override
    public boolean isBlocked(Long viewerId, Long targetUserId) {
        if (viewerId == null || targetUserId == null) {
            return false;
        }
        boolean viewerBlocksTarget = socialUserBlockMapper.existsBlock(viewerId, targetUserId);
        boolean targetBlocksViewer = socialUserBlockMapper.existsBlock(targetUserId, viewerId);
        return viewerBlocksTarget || targetBlocksViewer;
    }

    private Page<SocialUserFollow> buildPage(Long userId, Integer page, Integer pageSize) {
        if (userId == null) {
            throw new GloboxApplicationException(SocialCode.NOTE_NOT_FOUND);
        }
        long p = (page == null || page < 1) ? 1 : page;
        long ps = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        return new Page<>(p, ps);
    }

    private PaginationResult<FollowUserVo> buildFollowVoPage(Long userId,
                                                            List<SocialUserFollow> relations,
                                                            long total,
                                                            String keyword,
                                                            boolean isFollowingList) {
        if (CollectionUtils.isEmpty(relations)) {
            int pageSize = relations instanceof Page ? (int) ((Page<?>) relations).getSize() : relations.size();
            return PaginationResult.build(new ArrayList<>(), total, 1, pageSize);
        }
        List<Long> targetIds = relations.stream()
                .map(r -> isFollowingList ? r.getFollowUserId() : r.getUserId())
                .collect(Collectors.toList());

        // 批量查用户信息
        BatchUserInfoRequest req = new BatchUserInfoRequest();
        req.setUserIds(targetIds);
        BatchUserInfoResponse resp = userDubboService.batchGetUserInfo(req);
        Map<Long, UserInfoVo> userInfoMap = resp == null || CollectionUtils.isEmpty(resp.getUsers())
                ? new HashMap<>()
                : resp.getUsers().stream().collect(Collectors.toMap(UserInfoVo::getUserId, u -> u));

        // 反查我对对方的关注，标记 isFollowed/isMutual
        LambdaQueryWrapper<SocialUserFollow> myFollowQuery = new LambdaQueryWrapper<>();
        myFollowQuery.eq(SocialUserFollow::getUserId, userId)
                .in(SocialUserFollow::getFollowUserId, targetIds);
        List<SocialUserFollow> myFollows = socialUserFollowMapper.selectList(myFollowQuery);
        Set<Long> myFollowSet = myFollows.stream().map(SocialUserFollow::getFollowUserId).collect(Collectors.toSet());

        // 反查对方是否关注我，用于互关
        LambdaQueryWrapper<SocialUserFollow> reverseQuery = new LambdaQueryWrapper<>();
        reverseQuery.eq(SocialUserFollow::getFollowUserId, userId)
                .in(SocialUserFollow::getUserId, targetIds);
        List<SocialUserFollow> reverse = socialUserFollowMapper.selectList(reverseQuery);
        Set<Long> reverseSet = reverse.stream().map(SocialUserFollow::getUserId).collect(Collectors.toSet());

        List<FollowUserVo> voList = new ArrayList<>();
        for (SocialUserFollow rel : relations) {
            Long targetId = isFollowingList ? rel.getFollowUserId() : rel.getUserId();
            UserInfoVo info = userInfoMap.get(targetId);
            // 关键词仅在当前页过滤
            if (StringUtils.hasText(keyword) && info != null && !info.getNickName().contains(keyword)) {
                continue;
            }
            FollowUserVo vo = new FollowUserVo();
            vo.setUserId(targetId);
            if (info != null) {
                vo.setNickName(info.getNickName());
                vo.setAvatarUrl(info.getAvatarUrl());
            }
            vo.setIsFollowed(myFollowSet.contains(targetId));
            vo.setIsMutual(reverseSet.contains(targetId));
            vo.setFollowedAt(rel.getCreatedAt());
            voList.add(vo);
        }

        int currentPage = relations instanceof Page ? (int) ((Page<?>) relations).getCurrent() : 1;
        int pageSize = relations instanceof Page ? (int) ((Page<?>) relations).getSize() : voList.size();
        return PaginationResult.build(voList, total, currentPage, pageSize);
    }
}



