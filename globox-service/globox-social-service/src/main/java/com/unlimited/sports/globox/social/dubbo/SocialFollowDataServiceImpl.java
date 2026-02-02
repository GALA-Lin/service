package com.unlimited.sports.globox.social.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.SocialCode;
import com.unlimited.sports.globox.dubbo.social.ISocialFollowDataService;
import com.unlimited.sports.globox.model.social.entity.SocialUserFollow;
import com.unlimited.sports.globox.social.mapper.SocialUserFollowMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 社交关系数据RPC服务实现
 * 供搜索服务调用，获取用户粉丝数量数据
 */
@Component
@Slf4j
@DubboService(group = "rpc")
public class SocialFollowDataServiceImpl implements ISocialFollowDataService {

    @Autowired
    private SocialUserFollowMapper socialUserFollowMapper;

    /**
     * 批量获取用户粉丝数量
     *
     * @param userIds 用户ID列表
     * @return 粉丝数量映射：key为userId，value为粉丝数量
     */
    @Override
    public RpcResult<Map<Long, Integer>> getUserFollowerCounts(List<Long> userIds) {
        try {
            if (userIds == null || userIds.isEmpty()) {
                log.debug("获取粉丝数量: 用户ID列表为空");
                return RpcResult.ok(new HashMap<>());
            }
            log.info("批量获取用户粉丝数量: userIds数量={}", userIds.size());

            // 初始化结果Map，所有用户初值为0
            Map<Long, Integer> followerCounts = userIds.stream()
                    .collect(Collectors.toMap(userId -> userId, userId -> 0));
            QueryWrapper<SocialUserFollow> wrapper = new QueryWrapper<>();
            wrapper.in("follow_user_id", userIds)
                    .eq("deleted", false)
                    .select("follow_user_id", "COUNT(1) as count")
                    .groupBy("follow_user_id");
            List<Map<String, Object>> countResults = socialUserFollowMapper.selectMaps(wrapper);
            if (countResults != null && !countResults.isEmpty()) {
                countResults.forEach(row -> {
                    Long followUserId = ((Number) row.get("follow_user_id")).longValue();
                    Integer count = ((Number) row.get("count")).intValue();
                    followerCounts.put(followUserId, count);
                });
            }
            log.info("批量获取用户粉丝数量完成: 获取用户数={}, 有粉丝的用户数={}", userIds.size(),
                    (int) followerCounts.values().stream().filter(count -> count > 0).count());
            return RpcResult.ok(followerCounts);
        } catch (Exception e) {
            log.error("批量获取用户粉丝数量异常: userIds={}", userIds, e);
            return RpcResult.error(SocialCode.FOLLOWER_COUNT_FETCH_FAILED);
        }
    }
}
