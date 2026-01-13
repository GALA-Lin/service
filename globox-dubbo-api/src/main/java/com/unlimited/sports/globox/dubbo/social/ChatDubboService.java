package com.unlimited.sports.globox.dubbo.social;

import com.unlimited.sports.globox.common.result.RpcResult;

public interface ChatDubboService {
    RpcResult<Void> accountImport(String userId, String userName, String faceUrl);
}
