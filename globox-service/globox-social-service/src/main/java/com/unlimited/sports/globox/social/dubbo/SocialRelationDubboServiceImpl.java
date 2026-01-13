package com.unlimited.sports.globox.social.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.SocialCode;
import com.unlimited.sports.globox.dubbo.social.SocialRelationDubboService;
import com.unlimited.sports.globox.dubbo.social.dto.UserRelationStatusDto;
import com.unlimited.sports.globox.dubbo.social.dto.UserRelationStatusItemDto;
import com.unlimited.sports.globox.model.social.entity.SocialUserFollow;
import com.unlimited.sports.globox.social.mapper.SocialUserFollowMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@DubboService(group = "rpc")
public class SocialRelationDubboServiceImpl implements SocialRelationDubboService {

    @Autowired
    private SocialUserFollowMapper socialUserFollowMapper;

    @Override
    public RpcResult<UserRelationStatusDto> getRelationStatus(Long viewerId, Long targetUserId) {
        if (viewerId == null || targetUserId == null) {
            return RpcResult.error(SocialCode.RELATION_RPC_FAILED);
        }
        UserRelationStatusDto dto = new UserRelationStatusDto();
        if (viewerId.equals(targetUserId)) {
            dto.setIsFollowed(false);
            dto.setIsMutual(false);
            return RpcResult.ok(dto);
        }
        try {
            boolean viewerFollows = socialUserFollowMapper.existsFollow(viewerId, targetUserId);
            boolean targetFollows = socialUserFollowMapper.existsFollow(targetUserId, viewerId);
            dto.setIsFollowed(viewerFollows);
            dto.setIsMutual(viewerFollows && targetFollows);
            return RpcResult.ok(dto);
        } catch (Exception e) {
            log.error("RPC查询关注关系失败: viewerId={}, targetUserId={}", viewerId, targetUserId, e);
            return RpcResult.error(SocialCode.RELATION_RPC_FAILED);
        }
    }

    @Override
    public RpcResult<List<UserRelationStatusItemDto>> batchGetRelationStatus(Long viewerId, List<Long> targetUserIds) {
        if (viewerId == null) {
            return RpcResult.error(SocialCode.RELATION_RPC_FAILED);
        }
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            return RpcResult.ok(Collections.emptyList());
        }
        List<Long> distinctTargets = targetUserIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (distinctTargets.isEmpty()) {
            return RpcResult.ok(Collections.emptyList());
        }
        try {
            Set<Long> viewerFollowedTargets = socialUserFollowMapper.selectList(
                    new LambdaQueryWrapper<SocialUserFollow>()
                            .eq(SocialUserFollow::getUserId, viewerId)
                            .in(SocialUserFollow::getFollowUserId, distinctTargets)
            ).stream().map(SocialUserFollow::getFollowUserId).collect(Collectors.toSet());

            Set<Long> targetsFollowViewer = socialUserFollowMapper.selectList(
                    new LambdaQueryWrapper<SocialUserFollow>()
                            .eq(SocialUserFollow::getFollowUserId, viewerId)
                            .in(SocialUserFollow::getUserId, distinctTargets)
            ).stream().map(SocialUserFollow::getUserId).collect(Collectors.toSet());

            List<UserRelationStatusItemDto> result = distinctTargets.stream()
                    .map(targetId -> {
                        boolean isFollowed = !viewerId.equals(targetId) && viewerFollowedTargets.contains(targetId);
                        boolean isMutual = isFollowed && targetsFollowViewer.contains(targetId);
                        return UserRelationStatusItemDto.builder()
                                .targetUserId(targetId)
                                .isFollowed(isFollowed)
                                .isMutual(isMutual)
                                .build();
                    })
                    .collect(Collectors.toList());
            return RpcResult.ok(result);
        } catch (Exception e) {
            log.error("RPC批量查询关注关系失败: viewerId={}, targetUserIds={}", viewerId, targetUserIds, e);
            return RpcResult.error(SocialCode.RELATION_RPC_FAILED);
        }
    }
}
