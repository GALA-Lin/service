package com.unlimited.sports.globox.venue.adapter.impl;

import com.unlimited.sports.globox.model.venue.entity.venues.VenueThirdPartyConfig;
import com.unlimited.sports.globox.venue.adapter.ThirdPartyPlatformAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;

/**
 * 场小二平台适配器
 */
@Slf4j
@Component
public class ChangxiaoerAdapter implements ThirdPartyPlatformAdapter {

    @Autowired
    @Qualifier("thirdPartyRestTemplate")
    private RestTemplate restTemplate;

    private static final String PLATFORM_CODE = "changxiaoer";

    @Override
    public Map<String, Object> querySlots(VenueThirdPartyConfig config, LocalDate date) {
        log.info("[场小二] 查询槽位: venueId={}, thirdPartyVenueId={}, date={}",
                config.getVenueId(), config.getThirdPartyVenueId(), date);

        // TODO: 实现具体的API调用逻辑
        return null;
    }

    @Override
    public String lockSlot(VenueThirdPartyConfig config, String thirdPartyCourtId,
                          String startTime, String endTime, LocalDate date, String remark) {
        log.info("[场小二] 锁定槽位: venueId={}, courtId={}, date={}, time={}-{}",
                config.getVenueId(), thirdPartyCourtId, date, startTime, endTime);

        // TODO: 实现具体的API调用逻辑
        return null;
    }

    @Override
    public boolean unlockSlot(VenueThirdPartyConfig config, String thirdPartyBookingId) {
        log.info("[场小二] 解锁槽位: venueId={}, bookingId={}",
                config.getVenueId(), thirdPartyBookingId);

        // TODO: 实现具体的API调用逻辑
        return false;
    }

    @Override
    public String login(VenueThirdPartyConfig config) {
        log.info("[场小二] 开始登录: venueId={}, username={}",
                config.getVenueId(), config.getUsername());

        // TODO: 实现场小二登录逻辑
        // 1. 构建登录请求（使用 config.getUsername() 和 config.getPassword()）
        // 2. 发送HTTP POST请求到登录接口
        // 3. 解析响应获取Token
        // 4. 返回Token

        return null;
    }

    @Override
    public String getPlatformCode() {
        return PLATFORM_CODE;
    }
}
