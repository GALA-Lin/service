package com.unlimited.sports.globox.user.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 配置类
 * 配置 Bearer Token 认证支持
 *
 * @author Wreckloud
 * @since 2025/12/20
 */
@Configuration
public class SwaggerConfig {

    /**
     * 配置 OpenAPI 文档信息和安全方案
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GloBox 用户服务 API 文档")
                        .version("v1.0")
                        .description("GloBox 用户认证、登录、注册相关接口"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("请输入 JWT Token，格式：Bearer <token> 或直接输入 <token>")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
