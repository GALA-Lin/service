package com.unlimited.sports.globox.payment.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 分账 properties
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties("payment.profit-sharing")
public class ProfitSharingProperties {

    /**
     * 是否开启商家分账
     */
    private Boolean enableCoachProfitSharing;

    /**
     * 默认分账到教练
     */
    private CoachAccount defaultCoachAccount;

    /**
     * 百分比
     */
    private String profitSharingAmount;

    @Data
    public static class CoachAccount {
        private String account;
        private String realName;
    }
}
