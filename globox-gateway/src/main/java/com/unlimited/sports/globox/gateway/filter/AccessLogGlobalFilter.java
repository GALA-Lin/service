package com.unlimited.sports.globox.gateway.filter;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.constants.ResponseHeaderConstants;
import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.result.ApplicationCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

@Order(2)
@Slf4j
@Component
public class AccessLogGlobalFilter implements GlobalFilter {

    private static final String ATTR_START_TIME = "access_log_start_time";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        exchange.getAttributes().put(ATTR_START_TIME, start);

        ServerHttpRequest request = exchange.getRequest();

        String uri = request.getURI().toString();
        String path = request.getURI().getPath();
        String method = request.getMethodValue();

        String ip = resolveClientIp(request);
        HttpHeaders reqHeaders = request.getHeaders();

        String clientTypeStr = reqHeaders.getFirst(RequestHeaderConstants.HEADER_CLIENT_TYPE);
        ClientType clientType = ClientType.fromValue(clientTypeStr);

        return chain.filter(exchange).doFinally(signalType -> {
            Long st = exchange.getAttribute(ATTR_START_TIME);
            long costMs = st == null ? -1 : (System.currentTimeMillis() - st);

            Integer httpStatus = exchange.getResponse().getStatusCode() == null
                    ? null
                    : exchange.getResponse().getStatusCode().value();

            String bizCode = exchange.getResponse().getHeaders().getFirst(ResponseHeaderConstants.BUSINESS_CODE);
            bizCode = ObjectUtils.isEmpty(bizCode) ? ApplicationCode.UNKNOW.getCode().toString() : bizCode;
            String clientTypeName = clientType == null ? "UNKNOWN" : clientType.name();

            String operatorInfo = (clientType == null)
                    ? "clientTypeRaw=%s".formatted(safe(clientTypeStr))
                    : switch (clientType) {
                case APP ->
                        "userId=%s userRole=%s".formatted(
                                safe(reqHeaders.getFirst(RequestHeaderConstants.HEADER_USER_ID)),
                                safe(reqHeaders.getFirst(RequestHeaderConstants.HEADER_USER_ROLE))
                        );

                case JSAPI, THIRD_PARTY_JSAPI ->"userId=%s userRole=%s thirdPartyOpenid=%s".formatted(
                        safe(reqHeaders.getFirst(RequestHeaderConstants.HEADER_USER_ID)),
                        safe(reqHeaders.getFirst(RequestHeaderConstants.HEADER_USER_ROLE)),
                        safe(reqHeaders.getFirst(RequestHeaderConstants.HEADER_THIRD_PARTY_OPENID))
                );

                case MERCHANT ->
                        "merchantAccountId=%s merchantId=%s merchantUserId=%s merchantRole=%s employeeId=%s".formatted(
                                safe(reqHeaders.getFirst(RequestHeaderConstants.HEADER_MERCHANT_ACCOUNT_ID)),
                                safe(reqHeaders.getFirst(RequestHeaderConstants.HEADER_MERCHANT_ID)),
                                safe(reqHeaders.getFirst(RequestHeaderConstants.HEADER_MERCHANT_USER_ID)),
                                safe(reqHeaders.getFirst(RequestHeaderConstants.HEADER_MERCHANT_ROLE)),
                                safe(reqHeaders.getFirst(RequestHeaderConstants.HEADER_EMPLOYEE_ID))
                        );
            };

            log.info("access uri={} path={} method={} ip={} httpStatus={} bizCode={} costMs={} clientType={} operatorInfo=({})",
                    uri, path, method, ip, httpStatus, bizCode, costMs, clientTypeName, operatorInfo);
        });
    }

    /**
     * 空值统一打印为 "-"，避免日志里出现 null（你也可以改为直接返回 null）
     */
    private String safe(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }

    private String resolveClientIp(ServerHttpRequest request) {
        HttpHeaders h = request.getHeaders();

        String xff = h.getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int idx = xff.indexOf(',');
            return (idx > 0 ? xff.substring(0, idx) : xff).trim();
        }

        String xri = h.getFirst("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();

        InetSocketAddress remote = request.getRemoteAddress();
        if (remote == null) return "unknown";
        return Optional.ofNullable(remote.getAddress()).map(InetAddress::getHostAddress).orElse("unknown");
    }
}