package com.unlimited.sports.globox.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.SocialCode;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.NotificationSender;
import com.unlimited.sports.globox.common.enums.notification.NotificationEventEnum;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoRequest;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoResponse;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.model.social.entity.SocialNote;
import com.unlimited.sports.globox.model.social.entity.SocialUserBlock;
import com.unlimited.sports.globox.model.social.entity.SocialUserFollow;
import com.unlimited.sports.globox.model.social.vo.BlockUserVo;
import com.unlimited.sports.globox.model.social.vo.FollowUserVo;
import com.unlimited.sports.globox.model.social.vo.UserRelationStatsVo;
import com.unlimited.sports.globox.social.mapper.SocialNoteMapper;
import com.unlimited.sports.globox.social.mapper.SocialUserBlockMapper;
import com.unlimited.sports.globox.social.mapper.SocialUserFollowMapper;
import com.unlimited.sports.globox.social.service.SocialRelationService;
import com.unlimited.sports.globox.social.util.SocialNotificationUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
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

    @Autowired
    private NotificationSender notificationSender;

    @Autowired
    private SocialNotificationUtil socialNotificationUtil;

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> follow(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) {
            return R.error(SocialCode.USER_NOT_FOUND);
        }
        if (userId.equals(targetUserId)) {
            throw new GloboxApplicationException(SocialCode.FOLLOW_SELF_NOT_ALLOWED);
        }
        // 拉黑校验
        if (isBlocked(userId, targetUserId)) {
            throw new GloboxApplicationException(SocialCode.FOLLOW_DISABLED_BY_BLOCK);
        }

        // Soft-delete safety: check the unique key first to avoid duplicate key exceptions.
        LambdaQueryWrapper<SocialUserFollow> existingQuery = new LambdaQueryWrapper<>();
        existingQuery.eq(SocialUserFollow::getUserId, userId)
                .eq(SocialUserFollow::getFollowUserId, targetUserId)
                .last("LIMIT 1");
        SocialUserFollow existing = socialUserFollowMapper.selectOne(existingQuery);
        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getDeleted())) {
                existing.setDeleted(false);
                existing.setCreatedAt(LocalDateTime.now());
                socialUserFollowMapper.updateById(existing);
                socialNotificationUtil.sendFollowNotification(targetUserId, userId);
                log.info("Revived follow: userId={}, targetUserId={}", userId, targetUserId);
            }
            return R.ok("关注成功");
        }

        // No existing row; insert with a concurrency-safe fallback.
        try {
            SocialUserFollow follow = SocialUserFollow.builder()
                    .userId(userId)
                    .followUserId(targetUserId)
                    .createdAt(LocalDateTime.now())
                    .deleted(false)
                    .build();
            socialUserFollowMapper.insert(follow);
            
            // 插入成功，发送通知
            socialNotificationUtil.sendFollowNotification(targetUserId, userId);
            log.info("新增关注成功：userId={}, targetUserId={}", userId, targetUserId);
        } catch (DuplicateKeyException e) {
            // 并发插入冲突，当幂等成功处理
            log.info("并发关注冲突，幂等处理：userId={}, targetUserId={}", userId, targetUserId);
        }

        return R.ok("关注成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> unfollow(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) {
            return R.error(SocialCode.USER_NOT_FOUND);
        }
        
        // 条件更新：只更新 deleted=false 的记录
        LambdaUpdateWrapper<SocialUserFollow> cancelUpdate = new LambdaUpdateWrapper<>();
        cancelUpdate.eq(SocialUserFollow::getUserId, userId)
                .eq(SocialUserFollow::getFollowUserId, targetUserId)
                .eq(SocialUserFollow::getDeleted, false)
                .set(SocialUserFollow::getDeleted, true);
        int cancelledRows = socialUserFollowMapper.update(null, cancelUpdate);
        
        if (cancelledRows > 0) {
            log.info("取消关注成功：userId={}, targetUserId={}", userId, targetUserId);
        }
        // 无论是否取消成功，都幂等返回
        
        return R.ok("取消关注成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> block(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) {
            return R.error(SocialCode.USER_NOT_FOUND);
        }
        if (userId.equals(targetUserId)) {
            throw new GloboxApplicationException(SocialCode.FOLLOW_SELF_NOT_ALLOWED);
        }
        // Soft-delete safety: check the unique key first to avoid duplicate key exceptions.
        LambdaQueryWrapper<SocialUserBlock> existingQuery = new LambdaQueryWrapper<>();
        existingQuery.eq(SocialUserBlock::getUserId, userId)
                .eq(SocialUserBlock::getBlockedUserId, targetUserId)
                .last("LIMIT 1");
        SocialUserBlock existing = socialUserBlockMapper.selectOne(existingQuery);
        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getDeleted())) {
                existing.setDeleted(false);
                existing.setCreatedAt(LocalDateTime.now());
                socialUserBlockMapper.updateById(existing);
                log.info("Revived block: userId={}, targetUserId={}", userId, targetUserId);
            }
            // Ensure follow relations are removed even on idempotent calls.
            softDeleteBidirectionalFollow(userId, targetUserId);
            return R.ok("拉黑成功");
        }
        // No existing row; insert with a concurrency-safe fallback.
        try {
            SocialUserBlock block = SocialUserBlock.builder()
                    .userId(userId)
                    .blockedUserId(targetUserId)
                    .createdAt(LocalDateTime.now())
                    .deleted(false)
                    .build();
            socialUserBlockMapper.insert(block);
            
            // 插入成功，软删双向关注
            softDeleteBidirectionalFollow(userId, targetUserId);
            log.info("新增拉黑成功：userId={}, targetUserId={}", userId, targetUserId);
        } catch (DuplicateKeyException e) {
            // 并发插入冲突，当幂等成功处理
            log.info("并发拉黑冲突，幂等处理：userId={}, targetUserId={}", userId, targetUserId);
            // 即使冲突，也要确保双向关注被删除
            softDeleteBidirectionalFollow(userId, targetUserId);
        }

        return R.ok("拉黑成功");
    }

    /**
     * 软删双向关注关系
     */
    private void softDeleteBidirectionalFollow(Long userId, Long targetUserId) {
        // 软删双向关注
        LambdaQueryWrapper<SocialUserFollow> del1 = new LambdaQueryWrapper<>();
        del1.eq(SocialUserFollow::getUserId, userId)
                .eq(SocialUserFollow::getFollowUserId, targetUserId)
                .eq(SocialUserFollow::getDeleted, false);
        SocialUserFollow follow1 = socialUserFollowMapper.selectOne(del1);
        if (follow1 != null) {
            follow1.setDeleted(true);
            socialUserFollowMapper.updateById(follow1);
        }

        LambdaQueryWrapper<SocialUserFollow> del2 = new LambdaQueryWrapper<>();
        del2.eq(SocialUserFollow::getUserId, targetUserId)
                .eq(SocialUserFollow::getFollowUserId, userId)
                .eq(SocialUserFollow::getDeleted, false);
        SocialUserFollow follow2 = socialUserFollowMapper.selectOne(del2);
        if (follow2 != null) {
            follow2.setDeleted(true);
            socialUserFollowMapper.updateById(follow2);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> unblock(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }
        
        // 条件更新：只更新 deleted=false 的记录
        LambdaUpdateWrapper<SocialUserBlock> cancelUpdate = new LambdaUpdateWrapper<>();
        cancelUpdate.eq(SocialUserBlock::getUserId, userId)
                .eq(SocialUserBlock::getBlockedUserId, targetUserId)
                .eq(SocialUserBlock::getDeleted, false)
                .set(SocialUserBlock::getDeleted, true);
        int cancelledRows = socialUserBlockMapper.update(null, cancelUpdate);
        
        if (cancelledRows > 0) {
            log.info("取消拉黑成功：userId={}, targetUserId={}", userId, targetUserId);
        }
        // 无论是否取消成功，都幂等返回
        
        return R.ok("取消拉黑成功");
    }

    @Override
    public R<PaginationResult<FollowUserVo>> getFollowing(Long viewerId, Long targetUserId, Integer page, Integer pageSize, String keyword) {
        Long listOwnerId = targetUserId == null ? viewerId : targetUserId;
        Page<SocialUserFollow> pageParam = buildPage(listOwnerId, page, pageSize);
        Page<SocialUserFollow> pageResult = socialUserFollowMapper.selectFollowingPage(listOwnerId, pageParam);
        return R.ok(buildFollowVoPage(viewerId, pageResult.getRecords(), pageResult.getTotal(), keyword, true));
    }

    @Override
    public R<PaginationResult<FollowUserVo>> getFans(Long viewerId, Long targetUserId, Integer page, Integer pageSize, String keyword) {
        Long listOwnerId = targetUserId == null ? viewerId : targetUserId;
        Page<SocialUserFollow> pageParam = buildPage(listOwnerId, page, pageSize);
        Page<SocialUserFollow> pageResult = socialUserFollowMapper.selectFansPage(listOwnerId, pageParam);
        return R.ok(buildFollowVoPage(viewerId, pageResult.getRecords(), pageResult.getTotal(), keyword, false));
    }

    @Override
    public R<PaginationResult<FollowUserVo>> getMutual(Long viewerId, Long targetUserId, Integer page, Integer pageSize, String keyword) {
        Long listOwnerId = targetUserId == null ? viewerId : targetUserId;
        // 基于关注列表筛选互关
        Page<SocialUserFollow> pageParam = buildPage(listOwnerId, page, pageSize);
        Page<SocialUserFollow> followingPage = socialUserFollowMapper.selectFollowingPage(listOwnerId, pageParam);
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
        reverseQuery.eq(SocialUserFollow::getFollowUserId, listOwnerId)
                .eq(SocialUserFollow::getDeleted, false)
                .in(SocialUserFollow::getUserId, targetIds);
        List<SocialUserFollow> reverse = socialUserFollowMapper.selectList(reverseQuery);
        Set<Long> mutualSet = reverse.stream().map(SocialUserFollow::getUserId).collect(Collectors.toSet());
        List<SocialUserFollow> mutualList = following.stream()
                .filter(f -> mutualSet.contains(f.getFollowUserId()))
                .collect(Collectors.toList());
        // total 按当前页互关数量，简化处理
        return R.ok(buildFollowVoPage(viewerId, mutualList, mutualList.size(), keyword, true));
    }

    @Override
    public R<UserRelationStatsVo> getUserStats(Long targetUserId) {
        if (targetUserId == null) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }
        // 关注数
        LambdaQueryWrapper<SocialUserFollow> followCountQuery = new LambdaQueryWrapper<>();
        followCountQuery.eq(SocialUserFollow::getUserId, targetUserId)
                .eq(SocialUserFollow::getDeleted, false);
        long followCount = socialUserFollowMapper.selectCount(followCountQuery);

        // 粉丝数
        LambdaQueryWrapper<SocialUserFollow> fansCountQuery = new LambdaQueryWrapper<>();
        fansCountQuery.eq(SocialUserFollow::getFollowUserId, targetUserId)
                .eq(SocialUserFollow::getDeleted, false);
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

    private Page<SocialUserFollow> buildPage(Long ownerId, Integer page, Integer pageSize) {
        if (ownerId == null) {
            throw new GloboxApplicationException(SocialCode.NOTE_NOT_FOUND);
        }
        long p = (page == null || page < 1) ? 1 : page;
        long ps = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        return new Page<>(p, ps);
    }

    private PaginationResult<FollowUserVo> buildFollowVoPage(Long viewerId,
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
        RpcResult<BatchUserInfoResponse> rpcResult = userDubboService.batchGetUserInfo(req);
        Assert.rpcResultOk(rpcResult);
        BatchUserInfoResponse resp = rpcResult.getData();
        Map<Long, UserInfoVo> userInfoMap = resp == null || CollectionUtils.isEmpty(resp.getUsers())
                ? new HashMap<>()
                : resp.getUsers().stream().collect(Collectors.toMap(UserInfoVo::getUserId, u -> u));

        // 反查我对对方的关注，标记 isFollowed/isMutual
        LambdaQueryWrapper<SocialUserFollow> myFollowQuery = new LambdaQueryWrapper<>();
        myFollowQuery.eq(SocialUserFollow::getUserId, viewerId)
                .eq(SocialUserFollow::getDeleted, false)
                .in(SocialUserFollow::getFollowUserId, targetIds);
        List<SocialUserFollow> myFollows = socialUserFollowMapper.selectList(myFollowQuery);
        Set<Long> myFollowSet = myFollows.stream().map(SocialUserFollow::getFollowUserId).collect(Collectors.toSet());

        // 反查对方是否关注我，用于互关
        LambdaQueryWrapper<SocialUserFollow> reverseQuery = new LambdaQueryWrapper<>();
        reverseQuery.eq(SocialUserFollow::getFollowUserId, viewerId)
                .eq(SocialUserFollow::getDeleted, false)
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
            boolean followed = myFollowSet.contains(targetId);
            vo.setIsFollowed(followed);
            vo.setIsMutual(followed && reverseSet.contains(targetId));
            vo.setFollowedAt(rel.getCreatedAt());
            voList.add(vo);
        }

        int currentPage = relations instanceof Page ? (int) ((Page<?>) relations).getCurrent() : 1;
        int pageSize = relations instanceof Page ? (int) ((Page<?>) relations).getSize() : voList.size();
        return PaginationResult.build(voList, total, currentPage, pageSize);
    }

    @Override
    public R<PaginationResult<BlockUserVo>> getBlockedUsers(Long userId, Integer page, Integer pageSize, String keyword) {
        if (userId == null) {
            return R.error(SocialCode.USER_NOT_FOUND);
        }
        int p = (page == null || page < 1) ? 1 : page;
        int ps = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        Page<SocialUserBlock> mpPage = new Page<>(p, ps);
        LambdaQueryWrapper<SocialUserBlock> query = new LambdaQueryWrapper<>();
        query.eq(SocialUserBlock::getUserId, userId)
                .eq(SocialUserBlock::getDeleted, false)
                .orderByDesc(SocialUserBlock::getCreatedAt);
        socialUserBlockMapper.selectPage(mpPage, query);

        List<SocialUserBlock> blocks = mpPage.getRecords();
        List<BlockUserVo> voList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(blocks)) {
            List<Long> targetIds = blocks.stream()
                    .map(SocialUserBlock::getBlockedUserId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, UserInfoVo> userInfoMap = batchGetUserInfoMap(targetIds);

            for (SocialUserBlock b : blocks) {
                UserInfoVo info = userInfoMap.get(b.getBlockedUserId());
                if (StringUtils.hasText(keyword) && info != null && !info.getNickName().contains(keyword)) {
                    continue;
                }
                BlockUserVo vo = new BlockUserVo();
                vo.setUserId(b.getBlockedUserId());
                vo.setBlockedAt(b.getCreatedAt());
                if (info != null) {
                    vo.setNickName(info.getNickName());
                    vo.setAvatarUrl(info.getAvatarUrl());
                }
                voList.add(vo);
            }
        }

        boolean hasMore = mpPage.getCurrent() * mpPage.getSize() < mpPage.getTotal();
        PaginationResult<BlockUserVo> result = PaginationResult.build(voList, mpPage.getTotal(), p, ps);
        return R.ok(result);
    }

    private Map<Long, UserInfoVo> batchGetUserInfoMap(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return new HashMap<>();
        }
        BatchUserInfoRequest req = new BatchUserInfoRequest();
        req.setUserIds(userIds);
        RpcResult<BatchUserInfoResponse> rpcResult = userDubboService.batchGetUserInfo(req);
        Assert.rpcResultOk(rpcResult);
        BatchUserInfoResponse resp = rpcResult.getData();
        if (resp == null || CollectionUtils.isEmpty(resp.getUsers())) {
            return new HashMap<>();
        }
        return resp.getUsers().stream()
                .collect(Collectors.toMap(UserInfoVo::getUserId, u -> u, (a, b) -> a));
    }

}



