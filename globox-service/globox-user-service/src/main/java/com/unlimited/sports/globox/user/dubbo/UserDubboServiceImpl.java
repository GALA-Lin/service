package com.unlimited.sports.globox.user.dubbo;

import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoRequest;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoResponse;
import com.unlimited.sports.globox.dubbo.user.dto.UserInfoDto;
import com.unlimited.sports.globox.model.auth.entity.UserProfile;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.user.service.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户信息查询RPC服务实现
 *
 * @author Wreckloud
 * @since 2025/12/20
 */
@Component
@DubboService(group = "rpc")
@Slf4j
public class UserDubboServiceImpl implements UserDubboService {

    @Value("${user.profile.default-avatar-url:}")
    private String defaultAvatarUrl;

    @Autowired
    private UserProfileService userProfileService;

    @Override
    public RpcResult<UserInfoVo> getUserInfo(Long userId) {
        log.info("【RPC调用】查询单个用户信息, userId={}", userId);

        UserProfile profile = userProfileService.getUserProfileById(userId);
        if (profile == null) {
            log.info("【RPC调用】查询单个用户信息为空, userId={}", userId);
            return RpcResult.error(UserAuthCode.QUERY_NOT_EXIST);
        }

        // 转换为VO
        UserInfoVo vo = new UserInfoVo();
        BeanUtils.copyProperties(profile, vo);
        // 手动映射枚举和字段名不一致的字段
        vo.setGender(profile.getGender());
        vo.setUserNtrpLevel(profile.getNtrp() != null ? profile.getNtrp().doubleValue() : null);
        if (!StringUtils.hasText(vo.getAvatarUrl()) && StringUtils.hasText(defaultAvatarUrl)) {
            vo.setAvatarUrl(defaultAvatarUrl);
        }

        log.info("【RPC调用】查询单个用户信息成功, userId={}, nickName={}", 
                 userId, vo.getNickName());
        return RpcResult.ok(vo);
    }

    @Override
    public RpcResult<BatchUserInfoResponse> batchGetUserInfo(BatchUserInfoRequest request) {
        BatchUserInfoResponse response = new BatchUserInfoResponse();
        
        if (request == null || CollectionUtils.isEmpty(request.getUserIds())) {
            log.info("【RPC调用】批量查询用户信息, 请求数量=0");
            response.setUsers(Collections.emptyList());
            response.setUserCount(0);
            return RpcResult.ok(response);
        }

        List<Long> userIds = request.getUserIds();
        int requestCount = userIds.size();
        log.info("【RPC调用】批量查询用户信息, 请求数量={}", requestCount);

        // 批量查询用户资料
        List<UserProfile> profiles = userProfileService.batchGetUserProfile(userIds);

        if (CollectionUtils.isEmpty(profiles)) {
            log.info("【RPC调用】批量查询用户信息成功, 请求数量={}, 返回数量=0", requestCount);
            response.setUsers(Collections.emptyList());
            response.setUserCount(0);
            return RpcResult.ok(response);
        }

        // 转换为VO列表
        List<UserInfoVo> userInfoList = profiles.stream()
                .map(profile -> {
                    UserInfoVo dto = new UserInfoVo();
                    BeanUtils.copyProperties(profile, dto);
                    // 手动映射枚举和字段名不一致的字段
                    dto.setGender(profile.getGender());
                    dto.setUserNtrpLevel(profile.getNtrp() != null ? profile.getNtrp().doubleValue() : null);
                    if (!StringUtils.hasText(dto.getAvatarUrl()) && StringUtils.hasText(defaultAvatarUrl)) {
                        dto.setAvatarUrl(defaultAvatarUrl);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
        
        int responseCount = userInfoList.size();
        log.info("【RPC调用】批量查询用户信息成功, 请求数量={}, 返回数量={}", 
                 requestCount, responseCount);
        
        response.setUsers(userInfoList);
        response.setUserCount(responseCount);
        return RpcResult.ok(response);
    }
}
