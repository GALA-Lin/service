package com.unlimited.sports.globox.dubbo.config;

import com.alibaba.csp.sentinel.adapter.dubbo.config.DubboAdapterGlobalConfig;
import com.alibaba.csp.sentinel.adapter.dubbo.fallback.DubboFallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Configuration;


/**
 * dubbo 默认的 fallback 方法
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SentinelDubboFallbackAutoConfig implements SmartInitializingSingleton {

    private final DubboFallback dubboFallbackHandler;

    @Override
    public void afterSingletonsInstantiated() {
        DubboAdapterGlobalConfig.setConsumerFallback(dubboFallbackHandler);
        DubboAdapterGlobalConfig.setProviderFallback(dubboFallbackHandler);
    }

}
