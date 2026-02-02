package com.unlimited.sports.globox.common.utils;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.unlimited.sports.globox.common.result.ApplicationCode;
import com.unlimited.sports.globox.common.result.ResultCode;

/**
 * 限流相关工具类
 */
public final class LimitUtils {

    public static ResultCode getLimitTypeByException(BlockException e) {

        if (e instanceof DegradeException) {
            return ApplicationCode.DEGRADED;
        } else if (e instanceof FlowException) {
            return ApplicationCode.FLOW;
        } else if (e instanceof ParamFlowException) {
            return ApplicationCode.PARAM_FLOW;
        } else if (e instanceof SystemBlockException) {
            return ApplicationCode.SYSTEM_BLOCK;
        } else if (e instanceof AuthorityException) {
            return ApplicationCode.AUTHORITY;
        }
        return ApplicationCode.FAIL;
    }

}
