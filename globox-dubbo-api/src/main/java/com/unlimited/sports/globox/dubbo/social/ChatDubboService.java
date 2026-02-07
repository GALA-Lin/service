package com.unlimited.sports.globox.dubbo.social;

import com.unlimited.sports.globox.common.result.RpcResult;

import java.util.List;

public interface ChatDubboService {
    RpcResult<Void> accountImport(String userId, String userName, String faceUrl);

    void batchAccountImport(List<String> userIdList);
}
