package com.unlimited.sports.globox.gateway.filter;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
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
import org.springframework.http.MediaType;
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

    @Value("${jwt.secret}")
    private String jwtSecret;

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

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = JwtUtil.extractTokenFromHeader(authHeader);
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange, "Missing token");
        }

        if (!JwtUtil.validateToken(token, jwtSecret)) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        String userId = JwtUtil.getSubject(token, jwtSecret);
        String role = Optional.ofNullable(JwtUtil.getClaim(token, jwtSecret, "role", Object.class))
                .map(Object::toString)
                .orElse(null);

        if (!StringUtils.hasText(userId) || !StringUtils.hasText(role)) {
            return unauthorized(exchange, "Token missing subject or role");
        }

        // 覆盖同名头，防伪造
        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(headers -> {
                    headers.set(RequestHeaderConstants.HEADER_USER_ID, userId);
                    headers.set(RequestHeaderConstants.HEADER_USER_ROLE, role);
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isWhite(String path) {
        return authWhitelistProperties.getUrls().stream()
                .anyMatch(pattern -> matcher.match(pattern, path));
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
