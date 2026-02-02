package com.unlimited.sports.globox.user.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.dubbo.user.IUserSearchDataService;
import com.unlimited.sports.globox.model.auth.entity.UserProfile;
import com.unlimited.sports.globox.model.auth.vo.UserSyncVo;
import com.unlimited.sports.globox.user.mapper.UserProfileMapper;
import com.unlimited.sports.globox.user.service.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户搜索数据RPC服务实现
 * 提供用户数据增量同步功能
 */
@Component
@DubboService(group = "rpc")
@Slf4j
public class UserSearchDataServiceImpl implements IUserSearchDataService {

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Override
    public RpcResult<List<UserSyncVo>> syncUserData(LocalDateTime updatedTime) {
        try {
            log.info("开始同步用户数据: updatedTime={}", updatedTime);

            // 构建查询条件
            LambdaQueryWrapper<UserProfile> queryWrapper = new LambdaQueryWrapper<>();
            
            // 如果有更新时间，则增量同步
            if (updatedTime != null) {
                queryWrapper.ge(UserProfile::getUpdatedAt, updatedTime);
            }
            
            // 排除已注销用户（可选，根据业务需求决定是否同步已注销用户）
             queryWrapper.eq(UserProfile::getCancelled, false);

            List<UserProfile> userProfiles = userProfileMapper.selectList(queryWrapper);

            if (userProfiles == null || userProfiles.isEmpty()) {
                log.info("没有需要同步的用户数据");
                return RpcResult.ok(Collections.emptyList());
            }

            log.info("查询到用户数据: 数量={}", userProfiles.size());

            // 转换为UserSyncVo
            List<UserSyncVo> syncVos = userProfiles.stream()
                    .map(UserSyncVo::convertToSyncVo)
                    .collect(Collectors.toList());

            return RpcResult.ok(syncVos);

        } catch (Exception e) {
            log.error("同步用户数据异常: updatedTime={}", updatedTime, e);
            return RpcResult.error(UserAuthCode.SYNC_USER_PROFILE_ERROR);
        }
    }
}
