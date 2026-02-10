package com.unlimited.sports.globox.user.util;

import com.unlimited.sports.globox.common.constants.SearchMQConstants;
import com.unlimited.sports.globox.model.auth.entity.UserProfile;
import com.unlimited.sports.globox.model.auth.vo.UserSyncBatchMessage;
import com.unlimited.sports.globox.model.auth.vo.UserSyncVo;
import com.unlimited.sports.globox.common.service.MQService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户同步MQ消息发送器
 */
@Slf4j
@Component
public class UserSyncMQSender {

    @Autowired
    private MQService mqService;

    /**
     * 发送用户同步消息到MQ（支持批量）
     */
    @Async
    public void sendUserSyncMessage(List<UserProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            log.warn("[用户同步MQ] 发送记录为空");
            return;
        }

        try {
            List<UserSyncVo> syncVos = profiles.stream()
                    .filter(profile -> profile != null && profile.getUserId() != null)
                    .map(UserSyncVo::convertToSyncVo)
                    .toList();

            if (syncVos.isEmpty()) {
                log.warn("[用户同步MQ] 发送记录为空");
                return;
            }
            UserSyncBatchMessage message = new UserSyncBatchMessage(syncVos);
            mqService.send(
                    SearchMQConstants.EXCHANGE_TOPIC_SEARCH,
                    SearchMQConstants.ROUTING_USER_SYNC,
                    message
            );
            log.info("[用户同步MQ] 发送成功- count={}", syncVos.size());
        } catch (Exception e) {
            log.error("[用户同步MQ] 发送失败- count={}", profiles.size(), e);
        }
    }

    /**
     * 发送单个用户同步消息到MQ
     */
    @Async
    public void sendUserSyncMessage(UserProfile profile) {
        if (profile == null || profile.getUserId() == null) {
            log.warn("[用户同步MQ] 用户资料为空");
            return;
        }
        sendUserSyncMessage(List.of(profile));
    }
}
