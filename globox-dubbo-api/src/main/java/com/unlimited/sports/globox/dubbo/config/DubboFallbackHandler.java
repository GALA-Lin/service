package com.unlimited.sports.globox.dubbo.config;

import com.alibaba.csp.sentinel.adapter.dubbo.fallback.DubboFallback;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.unlimited.sports.globox.common.result.ApplicationCode;
import com.unlimited.sports.globox.common.result.ResultCode;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.LimitUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Dubbo 限流/降级/熔断处理器
 */
@Slf4j
@Component
public class DubboFallbackHandler implements DubboFallback {

    @Override
    public Result handle(Invoker<?> invoker, Invocation invocation, BlockException ex) {
        String service = invoker.getInterface().getName();
        String method = invocation.getMethodName();
        ResultCode resultCode = LimitUtils.getLimitTypeByException(ex);

        log.warn("Sentinel Dubbo block: type={}, service={}, method={}, args={}",
                resultCode.name(), service, method, invocation.getArguments());

        Class<?> returnType = resolveReturnType(invoker, invocation);

        if (RpcResult.class.isAssignableFrom(returnType)) {
            return AsyncRpcResult.newDefaultAsyncResult(RpcResult.ok(resultCode), invocation);
        }

        Object fallbackValue = defaultValue(returnType);
        return AsyncRpcResult.newDefaultAsyncResult(fallbackValue, invocation);
    }

    private Class<?> resolveReturnType(Invoker<?> invoker, Invocation invocation) {
        try {
            Class<?> iface = invoker.getInterface();
            Method m = iface.getMethod(invocation.getMethodName(), invocation.getParameterTypes());
            return m.getReturnType();
        } catch (Exception ignore) {
            return Object.class;
        }
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == void.class || returnType == Void.class) return null;
        if (returnType == boolean.class || returnType == Boolean.class) return false;
        if (returnType == byte.class || returnType == Byte.class) return (byte) 0;
        if (returnType == short.class || returnType == Short.class) return (short) 0;
        if (returnType == int.class || returnType == Integer.class) return 0;
        if (returnType == long.class || returnType == Long.class) return 0L;
        if (returnType == float.class || returnType == Float.class) return 0F;
        if (returnType == double.class || returnType == Double.class) return 0D;
        if (returnType == String.class) return "";
        return null;
    }
}
