package com.unlimited.sports.globox.gateway.filter;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.utils.JwtUtil;
import com.unlimited.sports.globox.gateway.prop.AuthWhitelistProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 全局鉴权 - 过滤器
 *
 * @author dk
 * @since 2025/12/19 12:42
 */
@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter {

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final AuthWhitelistProperties authWhitelistProperties;

    @Value("${auth.enabled:true}")
    private boolean authEnabled;

    @Value("${user.jwt.secret}")
    private String userJwtSecret;

    @Value("${merchant.jwt.secret}")
    private String merchantJwtSecret;

    @Value("${third-party.jwt.secret}")
    private String thirdPartyJwtSecret;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (!authEnabled) {
            return chain.filter(exchange);
        }

        if (isWhite(path)) {
            return chain.filter(exchange);
        }

        // 1. 获取并验证客户端类型
        String clientTypeValue = request.getHeaders().getFirst(RequestHeaderConstants.HEADER_CLIENT_TYPE);
        if (!StringUtils.hasText(clientTypeValue)) {
            return unauthorized(exchange, "Missing X-Client-Type header");
        }

        ClientType clientType = ClientType.fromValue(clientTypeValue);
        if (clientType == null) {
            return unauthorized(exchange, "Invalid X-Client-Type: " + clientTypeValue);
        }

        // 2. 根据客户端类型选择 JWT secret
        String secret = getSecretByClientType(clientType);
        if (!StringUtils.hasText(secret)) {
            return unauthorized(exchange, "Missing JWT secret for client type");
        }

        // 3. 验证 token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = JwtUtil.extractTokenFromHeader(authHeader);
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange, "Missing token");
        }

        if (!JwtUtil.validateToken(token, secret)) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        // 4. 解析 token
        String subject = JwtUtil.getSubject(token, secret);
        String role = Optional.ofNullable(JwtUtil.getClaim(token, secret, "role", Object.class))
                .map(Object::toString)
                .orElse(null);
        String tokenClientType = Optional.ofNullable(JwtUtil.getClaim(token, secret, "clientType", Object.class))
                .map(Object::toString)
                .orElse(null);
        String openid = Optional.ofNullable(JwtUtil.getClaim(token, secret, "openid", Object.class))
                .map(Object::toString)
                .orElse(null);

        if (!StringUtils.hasText(subject) || !StringUtils.hasText(role)) {
            return unauthorized(exchange, "Token missing subject or role");
        }
        if (StringUtils.hasText(tokenClientType) && !tokenClientType.equalsIgnoreCase(clientTypeValue)) {
            return unauthorized(exchange, "Token clientType mismatch");
        }

        // 5. 根据客户端类型注入对应的 headers
        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(headers -> injectHeaders(headers, clientType, subject, role, openid))
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isWhite(String path) {
        return authWhitelistProperties.getUrls().stream()
                .anyMatch(pattern -> matcher.match(pattern, path));
    }


    /**
     * 根据客户端类型注入对应的 headers（清除所有身份相关headers，防止伪造）
     */
    private void injectHeaders(HttpHeaders headers, ClientType clientType, String subject, String role, String openid) {
        headers.remove(RequestHeaderConstants.HEADER_USER_ID);
        headers.remove(RequestHeaderConstants.HEADER_USER_ROLE);
        headers.remove(RequestHeaderConstants.HEADER_MERCHANT_ID);
        headers.remove(RequestHeaderConstants.HEADER_MERCHANT_ROLE);
        headers.remove(RequestHeaderConstants.HEADER_THIRD_PARTY_ID);
        headers.remove(RequestHeaderConstants.HEADER_THIRD_PARTY_ROLE);
        headers.remove(RequestHeaderConstants.HEADER_THIRD_PARTY_OPENID);

        switch (clientType) {
            case APP, JSAPI, THIRD_PARTY_JSAPI -> {
                headers.set(RequestHeaderConstants.HEADER_USER_ID, subject);
                headers.set(RequestHeaderConstants.HEADER_USER_ROLE, role);
                if (StringUtils.hasText(openid)) {
                    headers.set(RequestHeaderConstants.HEADER_THIRD_PARTY_OPENID, openid);
                }
            }
            case MERCHANT -> {
                headers.set(RequestHeaderConstants.HEADER_MERCHANT_ID, subject);
                headers.set(RequestHeaderConstants.HEADER_MERCHANT_ROLE, role);
            }
        }
    }


    /**
     * 根据客户端类型获取对应的 JWT secret
     */
    private String getSecretByClientType(ClientType clientType) {
        return switch (clientType) {
            case APP, JSAPI -> userJwtSecret;
            case MERCHANT -> merchantJwtSecret;
            case THIRD_PARTY_JSAPI -> thirdPartyJwtSecret;
        };
    }

    /**
     * 根据客户端类型注入对应的 headers（清除所有身份相关 headers，防止伪造）
     */
    private void injectHeaders(HttpHeaders headers, ClientType clientType, String subject, String role) {
        // 清除所有身份相关 headers，防止伪造
        headers.remove(RequestHeaderConstants.HEADER_USER_ID);
        headers.remove(RequestHeaderConstants.HEADER_USER_ROLE);
        headers.remove(RequestHeaderConstants.HEADER_MERCHANT_ID);
        headers.remove(RequestHeaderConstants.HEADER_MERCHANT_ROLE);
        headers.remove(RequestHeaderConstants.HEADER_THIRD_PARTY_ID);
        headers.remove(RequestHeaderConstants.HEADER_THIRD_PARTY_ROLE);

        // 根据客户端类型注入对应的 headers
        switch (clientType) {
            case APP, JSAPI -> {
                headers.set(RequestHeaderConstants.HEADER_USER_ID, subject);
                headers.set(RequestHeaderConstants.HEADER_USER_ROLE, role);
            }
            case MERCHANT -> {
                headers.set(RequestHeaderConstants.HEADER_MERCHANT_ID, subject);
                headers.set(RequestHeaderConstants.HEADER_MERCHANT_ROLE, role);
            }
            case THIRD_PARTY_JSAPI -> {
                headers.set(RequestHeaderConstants.HEADER_THIRD_PARTY_ID, subject);
                headers.set(RequestHeaderConstants.HEADER_THIRD_PARTY_ROLE, role);
            }
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        String body = "{\"code\":401,\"message\":\"Unauthorized\"}";
        log.warn("JWT auth failed: {} - {}", reason, exchange.getRequest().getURI());
        var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
