package com.unlimited.sports.globox.social.dubbo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.SocialCode;
import com.unlimited.sports.globox.dubbo.social.ChatDubboService;
import com.unlimited.sports.globox.social.util.TencentCloudImUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 **/
@Slf4j
@Component
@DubboService(group = "rpc")
public class ChatDubboServiceImpl implements ChatDubboService {

    @Autowired
    TencentCloudImUtil tencentCloudImUtil;

    @Override
    public RpcResult<Void> accountImport(String userId, String userName, String faceUrl) {
        String result = tencentCloudImUtil.accountImport(userId, userName, faceUrl);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(result);
            String actionStatus = root.get("ActionStatus").asText();
            if ("OK".equals(actionStatus)) {
                return RpcResult.ok();
            } else {
                return RpcResult.error(SocialCode.IMPORT_USER_TO_IM_FAILED);
            }
        } catch (Exception e) {
            log.error("用户注册腾讯 IM 失败:{}", e.getMessage(), e);
            return RpcResult.error(SocialCode.IMPORT_USER_TO_IM_FAILED);
        }
    }

    @Override
    public void batchAccountImport(List<String> userIdList) {
        tencentCloudImUtil.multiAccountImport(userIdList);
    }
}
