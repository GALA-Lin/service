package com.unlimited.sports.globox.dubbo.order;

import com.unlimited.sports.globox.common.result.RpcResult;

import javax.validation.constraints.NotNull;

/**
 * 订单服务对用户服务提供 rpc 接口
 */
public interface OrderForUserDubboService {

    RpcResult<Void> checkOrderStatusBeforeUserCancel(@NotNull Long userId);
}
