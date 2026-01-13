package com.unlimited.sports.globox.dubbo.user.dto;

import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量用户信息响应DTO（RPC专用）
 * 用于批量查询用户信息，解决List序列化问题
 */
@Data
public class BatchUserInfoResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户信息列表
     */
    private List<UserInfoVo> users;

    /**
     * 本次查询到的用户数量
     */
    private Integer userCount;
}

