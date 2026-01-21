package com.unlimited.sports.globox.dubbo;

import com.unlimited.sports.globox.common.result.GovernanceCode;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.governance.SensitiveWordsDubboService;
import com.unlimited.sports.globox.service.SensitiveWordsService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 敏感词
 */
@Component
@DubboService(group = "rpc")
public class SensitiveWordsDubboServiceImpl implements SensitiveWordsDubboService {

    @Autowired
    private SensitiveWordsService sensitiveWordsService;


    @Override
    public RpcResult<Void> checkSensitiveWords(String content) {
        if (sensitiveWordsService.containsSensitiveWords(content)) {
            return RpcResult.error(GovernanceCode.CONTENT_NOT_ALLOW);
        } else {
            return RpcResult.ok();
        }
    }
}
