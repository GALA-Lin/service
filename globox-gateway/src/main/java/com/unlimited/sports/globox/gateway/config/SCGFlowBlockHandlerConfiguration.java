package com.unlimited.sports.globox.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.unlimited.sports.globox.common.result.ApplicationCode;
import com.unlimited.sports.globox.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.unlimited.sports.globox.common.constants.ResponseHeaderConstants.BUSINESS_CODE;

@Configuration
@RequiredArgsConstructor
public class SCGFlowBlockHandlerConfiguration {

    @Bean
    public BlockRequestHandler blockRequestHandler() {
        return (exchange, ex) -> {
            R<Void> body = R.error(ApplicationCode.GATEWAY_FLOW);

            return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(BUSINESS_CODE, String.valueOf(ApplicationCode.GATEWAY_FLOW.getCode()))
                    .bodyValue(body);
        };
    }

    /**
     * 所有 singleton 都初始化完成后再注册，避免被覆盖/时序问题
     */
    @Bean
    public SmartInitializingSingleton sentinelGatewayBlockHandlerInitializer(BlockRequestHandler blockRequestHandler) {
        return () -> GatewayCallbackManager.setBlockHandler(blockRequestHandler);
    }
}