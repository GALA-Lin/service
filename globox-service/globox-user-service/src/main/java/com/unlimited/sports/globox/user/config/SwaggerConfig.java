package com.unlimited.sports.globox.user.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomiser;
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

    /**
     * 全局 Header 说明：
     * - X-Client-Type：前端必传
     * - X-User-Id / X-Third-Party-Openid：由网关注入，客户端不要传（仅测试时手动传参）
     */
    @Bean
    public OpenApiCustomiser headerGlobalCustomiser() {
        return openApi -> openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("X-Client-Type")
                    .required(true)
                    .description("客户端类型：app/jsapi/merchant/third-party-jsapi")
                    .schema(new StringSchema()));

            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("X-User-Id")
                    .required(false)
                    .description("由网关注入，客户端不要传（仅测试时手动传参）")
                    .schema(new StringSchema()));

            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("X-Third-Party-Openid")
                    .required(false)
                    .description("由网关注入，客户端不要传（仅测试时手动传参）")
                    .schema(new StringSchema()));
        }));
    }

    @Bean
    public GroupedOpenApi uniappOpenApi(OpenApiCustomiser headerGlobalCustomiser) {
        return GroupedOpenApi.builder()
                .group("uniapp")
                .pathsToMatch("/auth/**", "/user/**")
                .pathsToExclude("/auth/merchant/**")
                .addOpenApiCustomiser(headerGlobalCustomiser)
                .build();
    }

    @Bean
    public GroupedOpenApi thirdPartyJsapiOpenApi(OpenApiCustomiser headerGlobalCustomiser) {
        return GroupedOpenApi.builder()
                .group("third-party-jsapi")
                .pathsToMatch("/auth/wechat/login", "/auth/wechat/phoneLogin")
                .addOpenApiCustomiser(headerGlobalCustomiser)
                .build();
    }
}
