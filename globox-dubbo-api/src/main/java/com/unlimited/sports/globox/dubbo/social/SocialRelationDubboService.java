package com.unlimited.sports.globox.dubbo.social;

import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.social.dto.UserRelationStatusDto;
import com.unlimited.sports.globox.dubbo.social.dto.UserRelationStatusItemDto;

import java.util.List;

public interface SocialRelationDubboService {

    /**
     * 查询关注与互相关注的状态
     */
    RpcResult<UserRelationStatusDto> getRelationStatus(Long viewerId, Long targetUserId);

    /**
     * 批量查询关注与互相关注的状态
     */
    RpcResult<List<UserRelationStatusItemDto>> batchGetRelationStatus(Long viewerId, List<Long> targetUserIds);

    /**
     * 检查是否存在任意方向的拉黑关系
     * @param viewerId 查看者用户ID
     * @param targetUserId 目标用户ID
     * @return true表示存在拉黑关系（任意一方拉黑了另一方），false表示不存在拉黑关系
     */
    RpcResult<Boolean> isBlocked(Long viewerId, Long targetUserId);
}
