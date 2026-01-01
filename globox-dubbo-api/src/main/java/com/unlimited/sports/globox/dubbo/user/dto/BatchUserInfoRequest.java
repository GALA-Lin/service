package com.unlimited.sports.globox.dubbo.user.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量用户信息请求DTO（RPC专用）
 * 用于批量查询用户信息，解决List序列化问题
 */
@Data
public class BatchUserInfoRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID列表
     */
    private List<Long> userIds;
}

