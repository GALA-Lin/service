package com.unlimited.sports.globox.dubbo.user;

import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoRequest;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoResponse;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.dubbo.user.dto.UserPhoneDto;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 用户信息查询RPC接口
 * 供其他服务调用，获取用户基本信息（头像、昵称等）
 */
@Validated
public interface UserDubboService {

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息，不存在返回null
     */
    RpcResult<UserInfoVo> getUserInfo(@Valid @NotNull(message = "查询的用户信息不能为空") Long userId);

    /**
     * 批量获取用户信息
     *
     * @param request 批量用户信息请求（最多50个用户ID）
     * @return 批量用户信息响应（不存在的用户会被过滤）
     */
    RpcResult<BatchUserInfoResponse> batchGetUserInfo(BatchUserInfoRequest request);

    /**
     * 获取用户手机号（明文）
     *
     * @param userId 用户ID
     * @return 用户手机号，不存在/未绑定返回 null
     */
    RpcResult<UserPhoneDto> getUserPhone(@Valid @NotNull(message = "查询的用户信息不能为空") Long userId);

    /**
     * 批量获取用户手机号（明文）
     *
     * @param userIds 用户ID列表（最多50个）
     * @return 用户手机号列表，未绑定的会被过滤
     */
    RpcResult<List<UserPhoneDto>> batchGetUserPhone(@Valid @NotNull(message = "查询的用户信息不能为空") List<Long> userIds);
}
