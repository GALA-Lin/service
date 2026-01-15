package com.unlimited.sports.globox.user.dubbo;

import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoRequest;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoResponse;
import com.unlimited.sports.globox.dubbo.user.dto.UserInfoDto;
import com.unlimited.sports.globox.dubbo.user.dto.UserPhoneDto;
import com.unlimited.sports.globox.model.auth.entity.UserProfile;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.model.auth.entity.AuthIdentity;
import com.unlimited.sports.globox.user.service.UserProfileService;
import com.unlimited.sports.globox.user.mapper.AuthIdentityMapper;
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

    @Autowired
    private AuthIdentityMapper authIdentityMapper;

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

    @Override
    public RpcResult<UserPhoneDto> getUserPhone(Long userId) {
        log.info("【RPC调用】查询用户手机号, userId={}", userId);
        AuthIdentity identity = authIdentityMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<AuthIdentity>lambdaQuery()
                        .eq(AuthIdentity::getUserId, userId)
                        .eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.PHONE)
                        .eq(AuthIdentity::getVerified, true)
        );
        if (identity == null) {
            return RpcResult.ok(null);
        }
        return RpcResult.ok(UserPhoneDto.builder()
                .userId(userId)
                .phone(identity.getIdentifier())
                .build());
    }

    @Override
    public RpcResult<List<UserPhoneDto>> batchGetUserPhone(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return RpcResult.ok(Collections.emptyList());
        }
        List<Long> distinctIds = userIds.stream()
                .filter(id -> id != null)
                .distinct()
                .limit(50)
                .collect(Collectors.toList());
        List<AuthIdentity> identities = authIdentityMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<AuthIdentity>lambdaQuery()
                        .in(AuthIdentity::getUserId, distinctIds)
                        .eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.PHONE)
                        .eq(AuthIdentity::getVerified, true)
        );
        if (CollectionUtils.isEmpty(identities)) {
            return RpcResult.ok(Collections.emptyList());
        }
        List<UserPhoneDto> result = identities.stream()
                .map(i -> UserPhoneDto.builder()
                        .userId(i.getUserId())
                        .phone(i.getIdentifier())
                        .build())
                .collect(Collectors.toList());
        return RpcResult.ok(result);
    }
}
