package com.unlimited.sports.globox.venue.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 第三方平台适配器工厂
 */
@Slf4j
@Component
public class ThirdPartyPlatformAdapterFactory {

    private final Map<String, ThirdPartyPlatformAdapter> adapterMap = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private List<ThirdPartyPlatformAdapter> adapters;

    @PostConstruct
    public void init() {
        if (adapters != null && !adapters.isEmpty()) {
            for (ThirdPartyPlatformAdapter adapter : adapters) {
                String platformCode = adapter.getPlatformCode();
                adapterMap.put(platformCode, adapter);
                log.info("注册第三方平台适配器: {}", platformCode);
            }
        }
    }

    /**
     * 根据平台代码获取适配器
     *
     * @param platformCode 平台代码
     * @return 适配器实例
     */
    public ThirdPartyPlatformAdapter getAdapter(String platformCode) {
        ThirdPartyPlatformAdapter adapter = adapterMap.get(platformCode);
        if (adapter == null) {
            throw new IllegalArgumentException("不支持的第三方平台: " + platformCode);
        }
        return adapter;
    }
}
