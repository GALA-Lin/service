package com.unlimited.sports.globox.demo.dubbo;

import com.unlimited.sports.globox.dubbo.demo.ProducerDubboService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

/**
 * producer demo 模块 - dubbo 实现类
 *
 * @author dk
 * @since 2025/12/18 10:09
 */
@Component
@DubboService(group = "rpc")
public class ProducerDubboServiceImpl implements ProducerDubboService {
    @Override
    public String sayHello(String name) {
        return "hello %s".formatted(name);
    }
}
