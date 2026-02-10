package com.unlimited.sports.globox.search.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;

/**
 * XXL-JOB 配置属性类
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "xxl.job")
public class XxlJobProperties {

    /**
     * 是否启用XXL-JOB
     */
    private Boolean enable;

    /**
     * XXL-JOB管理员配置
     */
    private Admin admin = new Admin();

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 执行器配置
     */
    private Executor executor = new Executor();



    @PostConstruct
    public void validate() {
        Assert.notNull(enable, "xxl.job.enable 配置不能为空");
        Assert.hasText(admin.getAddresses(), "xxl.job.admin.addresses 配置不能为空");
        Assert.hasText(accessToken, "xxl.job.accessToken 配置不能为空");
        Assert.notNull(executor.getPort(), "xxl.job.executor.port 配置不能为空");
        Assert.hasText(executor.getLogpath(), "xxl.job.executor.logpath 配置不能为空");
    }

    @Data
    public static class Admin {
        /**
         * 调度中心部署跟地址
         */
        private String addresses;
    }

    @Data
    public static class Executor {
        /**
         * 执行器端口号
         */
        private Integer port;

        /**
         * 执行器日志文件存储磁盘路径
         */
        private String logpath;

        /**
         * 执行器地址
         */
        private String address;

        /**
         * 执行器IP
         */
        private String ip;
    }

}
