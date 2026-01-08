package com.unlimited.sports.globox.dubbo.user;

import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoRequest;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoResponse;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;

/**
 * 用户信息查询RPC接口
 * 供其他服务调用，获取用户基本信息（头像、昵称等）
 */
public interface UserDubboService {

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息，不存在返回null
     */
    UserInfoVo getUserInfo(Long userId);

    /**
     * 批量获取用户信息
     *
     * @param request 批量用户信息请求（最多50个用户ID）
     * @return 批量用户信息响应（不存在的用户会被过滤）
     */
    BatchUserInfoResponse batchGetUserInfo(BatchUserInfoRequest request);
}
