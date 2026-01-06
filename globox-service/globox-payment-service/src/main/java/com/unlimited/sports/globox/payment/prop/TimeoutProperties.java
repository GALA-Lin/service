package com.unlimited.sports.globox.payment.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 订单超时配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "timeout")
public class TimeoutProperties {

    private Integer normal;

    private Integer activity;

}
