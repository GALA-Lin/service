package com.unlimited.sports.globox.dubbo.governance;

import com.unlimited.sports.globox.common.result.RpcResult;

/**
 * 敏感词相关 dubbo 接口
 */
public interface SensitiveWordsDubboService {
    RpcResult<Void> checkSensitiveWords(String content);
}
