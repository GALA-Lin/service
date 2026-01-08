package com.unlimited.sports.globox.social.dubbo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unlimited.sports.globox.dubbo.social.ChatDubboService;
import com.unlimited.sports.globox.social.util.TencentCloudImUtil;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 **/
@Component
@DubboService(group = "rpc")
public class ChatDubboServiceImpl implements ChatDubboService {

    @Autowired
    TencentCloudImUtil tencentCloudImUtil;

    @Override
    public Boolean accountImport(String userId, String userName, String faceUrl) {
        String result = tencentCloudImUtil.accountImport(userId, userName, faceUrl);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(result);
            String actionStatus = root.get("ActionStatus").asText();
            if ("OK".equals(actionStatus)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
