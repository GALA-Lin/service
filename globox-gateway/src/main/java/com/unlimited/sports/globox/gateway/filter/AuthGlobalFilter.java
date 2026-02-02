package com.unlimited.sports.globox.gateway.filter;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.constants.RedisKeyConstants;
import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.utils.JwtUtil;
import com.unlimited.sports.globox.gateway.prop.AuthWhitelistProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate stringRedisTemplate;

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

        // 白名单：如果带 token，则解析并注入 headers；不带 token 则直接放行
        if (isWhite(path)) {
            return handleWhitelistWithOptionalToken(exchange, chain);
        }

        // 非白名单强校验
        // 1) 获取并验证客户端类型
        String clientTypeValue = request.getHeaders().getFirst(RequestHeaderConstants.HEADER_CLIENT_TYPE);
        if (!StringUtils.hasText(clientTypeValue)) {
            return unauthorized(exchange, "Missing X-Client-Type header");
        }

        ClientType clientType = ClientType.fromValue(clientTypeValue);
        if (clientType == null) {
            return unauthorized(exchange, "Invalid X-Client-Type: " + clientTypeValue);
        }

        // 2) 根据客户端类型选择 JWT secret
        String secret = getSecretByClientType(clientType);
        if (!StringUtils.hasText(secret)) {
            return unauthorized(exchange, "Missing JWT secret for client type");
        }

        // 3) 验证 token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = JwtUtil.extractTokenFromHeader(authHeader);
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange, "Missing token");
        }

        if (!JwtUtil.validateToken(token, secret)) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        // 4) 解析 token
        JwtClaims claims;
        try {
            claims = parseClaims(token, secret);
        } catch (Exception e) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        // 必要字段校验
        if (!StringUtils.hasText(claims.subject) || !StringUtils.hasText(claims.role)) {
            return unauthorized(exchange, "Token missing subject or role");
        }

        // 商户端必须包含员工信息
        if (ClientType.MERCHANT.equals(clientType) && claims.staffId == null) {
            return unauthorized(exchange, "Token missing merchant info");
        }

        // token clientType 校验（若 token 内有 clientType）
        if (StringUtils.hasText(claims.tokenClientType) && !claims.tokenClientType.equalsIgnoreCase(clientTypeValue)) {
            return unauthorized(exchange, "Token clientType mismatch");
        }

        // APP jti 强校验（被踢下线/撤销）
        if (!validateAppJtiIfPresent(clientType, clientType.getValue(), claims.subject, claims.jti)) {
            return unauthorized(exchange, "Token revoked");
        }

        // 5) 注入 headers 并放行
        ServerWebExchange mutatedExchange = mutateExchangeWithHeaders(exchange, clientType, claims);
        return chain.filter(mutatedExchange);
    }

    /**
     * 白名单处理：存在 token -> 尝试解析并注入 headers；无 token -> 直接放行
     *
     * 设计原则：
     * - 不改变白名单放行语义：没有 token 时完全不影响。
     * - 有 token 时尽量复用解析逻辑，注入身份头，方便下游“可选用户态”场景。
     * - 白名单不应因 token 解析失败而拒绝访问：解析失败就当没 token（直接放行）。
     */
    private Mono<Void> handleWhitelistWithOptionalToken(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = JwtUtil.extractTokenFromHeader(authHeader);
        if (!StringUtils.hasText(token)) {
            return chain.filter(exchange);
        }

        String clientTypeValue = request.getHeaders().getFirst(RequestHeaderConstants.HEADER_CLIENT_TYPE);
        ClientType clientType = StringUtils.hasText(clientTypeValue) ? ClientType.fromValue(clientTypeValue) : null;

        // 白名单：没有 clientType 就不解析（避免猜 secret），直接放行
        if (clientType == null) {
            return chain.filter(exchange);
        }

        String secret = getSecretByClientType(clientType);

        return tryParseAndMutateExchange(exchange, token, clientType, secret)
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    /**
     * 白名单场景：尝试解析 token 并生成注入身份后的 exchange
     * - 成功：返回 Mono.just(mutatedExchange)
     * - 失败：返回 Mono.empty()（上层 defaultIfEmpty 仍会放行）
     */
    private Mono<ServerWebExchange> tryParseAndMutateExchange(
            ServerWebExchange exchange,
            String token,
            ClientType clientType,
            String secret
    ) {
        if (!StringUtils.hasText(secret)) {
            return Mono.empty();
        }

        try {
            if (!JwtUtil.validateToken(token, secret)) {
                return Mono.empty();
            }

            JwtClaims claims = parseClaims(token, secret);

            // 白名单：关键信息缺失 => 不注入
            if (!StringUtils.hasText(claims.subject) || !StringUtils.hasText(claims.role)) {
                return Mono.empty();
            }

            // 白名单：如果 token 内有 clientType，则需与 header clientType 一致，否则不注入
            if (StringUtils.hasText(claims.tokenClientType)
                    && !claims.tokenClientType.equalsIgnoreCase(clientType.getValue())) {
                return Mono.empty();
            }

            // 白名单：商户端缺 staffId 则不注入
            if (ClientType.MERCHANT.equals(clientType) && claims.staffId == null) {
                return Mono.empty();
            }

            // 白名单：APP jti 可选校验（不通过则不注入，但不拦截）
            if (!validateAppJtiIfPresent(clientType, clientType.getValue(), claims.subject, claims.jti)) {
                return Mono.empty();
            }

            return Mono.just(mutateExchangeWithHeaders(exchange, clientType, claims));
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    /**
     * 统一解析 claim，集中管理 key 名，避免重复代码
     */
    private JwtClaims parseClaims(String token, String secret) {
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

        String jti = Optional.ofNullable(JwtUtil.getClaim(token, secret, "jti", Object.class))
                .map(Object::toString)
                .orElse(null);

        Long staffId = getLongClaim(token, secret, "employee_id");
        Long merchantId = getLongClaim(token, secret, "merchant_id");

        return new JwtClaims(subject, role, openid, tokenClientType, jti, staffId, merchantId);
    }

    /**
     * APP 端：jti 可选校验
     * - 非 APP：直接通过
     * - jti 为空：直接通过
     * - 有 jti：必须与 Redis 中缓存一致才通过
     */
    private boolean validateAppJtiIfPresent(ClientType clientType, String clientTypeValueForKey, String subject, String jti) {
        if (!ClientType.APP.equals(clientType)) {
            return true;
        }
        if (!StringUtils.hasText(jti)) {
            return true;
        }

        String key = RedisKeyConstants.ACCESS_TOKEN_JTI_PREFIX + clientTypeValueForKey + ":" + subject;
        try {
            String cachedJti = stringRedisTemplate.opsForValue().get(key);
            return StringUtils.hasText(cachedJti) && jti.equals(cachedJti);
        } catch (Exception ignore) {
            return false;
        }
    }

    /**
     * 根据 clientType + claims 注入 headers，返回新的 exchange
     */
    private ServerWebExchange mutateExchangeWithHeaders(ServerWebExchange exchange, ClientType clientType, JwtClaims claims) {
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> injectHeaders(headers, clientType, claims.subject, claims.role, claims.openid, claims.staffId, claims.merchantId))
                .build();
        return exchange.mutate().request(mutatedRequest).build();
    }

    private boolean isWhite(String path) {
        return authWhitelistProperties.getUrls().stream()
                .anyMatch(pattern -> matcher.match(pattern, path));
    }

    private Long getLongClaim(String token, String secret, String key) {
        Object value = JwtUtil.getClaim(token, secret, key, Object.class);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 根据客户端类型注入对应的 headers（清除所有身份相关headers，防止伪造）
     */
    private void injectHeaders(HttpHeaders headers, ClientType clientType, String subject, String role, String openid, Long staffId, Long merchantId) {
        headers.remove(RequestHeaderConstants.HEADER_USER_ID);
        headers.remove(RequestHeaderConstants.HEADER_USER_ROLE);
        headers.remove(RequestHeaderConstants.HEADER_MERCHANT_ACCOUNT_ID);
        headers.remove(RequestHeaderConstants.HEADER_MERCHANT_ID);
        headers.remove(RequestHeaderConstants.HEADER_MERCHANT_USER_ID);
        headers.remove(RequestHeaderConstants.HEADER_MERCHANT_ROLE);
        headers.remove(RequestHeaderConstants.HEADER_THIRD_PARTY_ID);
        headers.remove(RequestHeaderConstants.HEADER_THIRD_PARTY_ROLE);
        headers.remove(RequestHeaderConstants.HEADER_THIRD_PARTY_OPENID);
        headers.remove(RequestHeaderConstants.HEADER_CLIENT_TYPE);
        headers.remove(RequestHeaderConstants.HEADER_EMPLOYEE_ID);

        switch (clientType) {
            case APP, JSAPI, THIRD_PARTY_JSAPI -> {
                headers.set(RequestHeaderConstants.HEADER_USER_ID, subject);
                headers.set(RequestHeaderConstants.HEADER_USER_ROLE, role);
                if (StringUtils.hasText(openid)) {
                    headers.set(RequestHeaderConstants.HEADER_THIRD_PARTY_OPENID, openid);
                }
            }
            case MERCHANT -> {
                headers.set(RequestHeaderConstants.HEADER_MERCHANT_ACCOUNT_ID, subject);
                Long resolvedMerchantId = merchantId != null ? merchantId : Long.parseLong(subject);
                headers.set(RequestHeaderConstants.HEADER_MERCHANT_ID, String.valueOf(resolvedMerchantId));
                headers.set(RequestHeaderConstants.HEADER_MERCHANT_USER_ID, subject); // 兼容第三方服务的user_id需求
                headers.set(RequestHeaderConstants.HEADER_MERCHANT_ROLE, role);
                headers.set(RequestHeaderConstants.HEADER_EMPLOYEE_ID, String.valueOf(staffId));
            }
        }

        // 添加端标识（用标准值覆盖）
        headers.set(RequestHeaderConstants.HEADER_CLIENT_TYPE, clientType.getValue());
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

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        String body = "{\"code\":401,\"message\":\"Unauthorized\"}";
        log.warn("JWT auth failed: {} - {}", reason, exchange.getRequest().getURI());
        var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * Token Claims 容器
     */
    private static final class JwtClaims {
        final String subject;
        final String role;
        final String openid;
        final String tokenClientType;
        final String jti;
        final Long staffId;
        final Long merchantId;

        private JwtClaims(String subject, String role, String openid, String tokenClientType, String jti, Long staffId, Long merchantId) {
            this.subject = subject;
            this.role = role;
            this.openid = openid;
            this.tokenClientType = tokenClientType;
            this.jti = jti;
            this.staffId = staffId;
            this.merchantId = merchantId;
        }
    }
}
