package com.unlimited.sports.globox.common.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import com.unlimited.sports.globox.common.prop.CosProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云COS配置
 */
@Configuration
@ConditionalOnBean(CosProperties.class)
public class CosConfig {

    @Autowired
    private CosProperties cosProperties;

    @Bean
    public COSClient cosClient() {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials(
                cosProperties.getSecretId(),
                cosProperties.getSecretKey()
        );

        // 2 设置bucket的地域
        Region region = new Region(cosProperties.getRegion());
        ClientConfig clientConfig = new ClientConfig(region);

        // 3 生成cos客户端
        return new COSClient(cred, clientConfig);
    }
}

