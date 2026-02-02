package com.unlimited.sports.globox.dubbo.social;

import com.unlimited.sports.globox.common.result.RpcResult;

import java.util.List;
import java.util.Map;

/**
 * 社交关系数据RPC服务接口
 * 供搜索服务调用，获取用户粉丝数量数据
 */
public interface ISocialFollowDataService {

    /**
     * 批量获取用户粉丝数量
     * 业务逻辑：查询social_follow表，统计每个用户被关注的数量
     * @param userIds 用户ID列表
     * @return 粉丝数量映射：key为userId，value为粉丝数量
     */
    RpcResult<Map<Long, Integer>> getUserFollowerCounts(List<Long> userIds);
}
