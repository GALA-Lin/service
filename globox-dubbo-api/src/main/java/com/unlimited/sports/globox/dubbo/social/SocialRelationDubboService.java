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
}
