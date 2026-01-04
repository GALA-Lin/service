package com.unlimited.sports.globox.common.result;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * rpc result
 */
@Getter
public class RpcResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = -8749812904663796797L;

    private boolean success;

    private T data;

    private final ResultCode resultCode;

    public RpcResult(ResultCode resultCode, T data) {
        this.resultCode = resultCode;
        this.data = data;
    }

    public RpcResult(ResultCode resultCode) {
        this.resultCode = resultCode;
    }

    /**
     * 成功
     */
    public static <T> RpcResult<T> ok() {
        RpcResult<T> result = new RpcResult<>(ApplicationCode.SUCCESS);
        result.success = true;
        return result;
    }

    /**
     * 成功
     */
    public static <T> RpcResult<T> ok(T data) {
        RpcResult<T> result = new RpcResult<>(ApplicationCode.SUCCESS, data);
        result.success = true;
        return result;
    }

    /**
     * 失败
     */
    public static <T> RpcResult<T> error(ResultCode code) {
        RpcResult<T> rpcResult = new RpcResult<>(code);
        rpcResult.success = false;
        return rpcResult;
    }
}
