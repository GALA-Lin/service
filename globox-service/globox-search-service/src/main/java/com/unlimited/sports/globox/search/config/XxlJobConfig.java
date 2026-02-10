package com.unlimited.sports.globox.search.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-JOB 配置
 */
@Slf4j
@Configuration
public class XxlJobConfig {

    @Autowired
    private XxlJobProperties xxlJobProperties;

    @Value("${spring.application.name}")
    private String appname;
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        String adminAddresses = xxlJobProperties.getAdmin().getAddresses();
        String accessToken = xxlJobProperties.getAccessToken();
        Integer port = xxlJobProperties.getExecutor().getPort();
        String logPath = xxlJobProperties.getExecutor().getLogpath();
        String address = xxlJobProperties.getExecutor().getAddress();
        String ip = xxlJobProperties.getExecutor().getIp();
        log.info(">>>>>>>>>>> xxl-job config init: adminAddresses={}, appname={}, port={}, logPath={}, address={}, ip={}",
                adminAddresses, appname, port, logPath, address, ip);
        
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setAddress(address);
        xxlJobSpringExecutor.setIp(ip);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setLogPath(logPath);
        return xxlJobSpringExecutor;
    }
}
