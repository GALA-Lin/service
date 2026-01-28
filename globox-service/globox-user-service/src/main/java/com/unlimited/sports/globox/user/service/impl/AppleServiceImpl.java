package com.unlimited.sports.globox.user.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.user.config.AppleProperties;
import com.unlimited.sports.globox.user.service.AppleService;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Apple登录服务实现类
 */
@Service
@Slf4j
public class AppleServiceImpl implements AppleService {

    @Autowired
    private AppleProperties appleProperties;

    /**
     * 是否使用Mock模式（自动检测结果）
     */
    private boolean useMockMode;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, RSAPublicKey> applePublicKeys = new HashMap<>();
    private long applePublicKeysExpireAt;
    private static final long APPLE_PUBLIC_KEYS_CACHE_SECONDS = 3600;

    /**
     * 初始化：检测配置并决定使用Mock模式还是真实验证模式
     */
    @PostConstruct
    public void init() {
        if (appleProperties.getEnabled() == null || !appleProperties.getEnabled()) {
            useMockMode = true;
            log.warn("【Apple服务】apple.enabled=false，强制使用Mock模式");
            return;
        }

        // 如果强制启用Mock模式，直接使用Mock
        if (appleProperties.getMockEnabled() != null && appleProperties.getMockEnabled()) {
            useMockMode = true;
            log.info("【Apple服务】强制启用Mock模式（apple.mock-enabled=true）");
            return;
        }

        // 非Mock模式下，严格依赖配置
        useMockMode = false;
        if (!StringUtils.hasText(appleProperties.getClientId()) && !StringUtils.hasText(appleProperties.getServiceId())) {
            log.warn("【Apple服务】apple.client-id或apple.service-id未配置，将使用Mock模式");
            useMockMode = true;
        }
    }

    @Override
    public String verifyAndExtractSub(String identityToken) {
        // 1. 基础校验
        if (!StringUtils.hasText(identityToken)) {
            log.error("【Apple服务】identityToken为空");
            throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
        }

        // 2. Mock模式（本地开发用）
        if (useMockMode) {
            // 从 token 中提取部分作为 mock sub
            String mockSub = "mock-apple-" + identityToken.substring(0, Math.min(20, identityToken.length()));
            log.info("【Mock Apple】验证成功：identityToken={}, mockSub={}", identityToken.substring(0, 20) + "...", mockSub);
            return mockSub;
        }

        // 3. 真实验证模式
        try {
            return verifyIdentityToken(identityToken);
        } catch (GloboxApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("【Apple服务】验证identityToken异常：error={}", e.getMessage(), e);
            throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM, e);
        }
    }

    /**
     * 验证 Apple identityToken（JWT格式）
     * 
     * @param identityToken JWT格式的token
     * @return Apple用户唯一标识（sub）
     */
    private String verifyIdentityToken(String identityToken) throws Exception {
        // 1. 解析JWT（header.payload.signature）
        String[] parts = identityToken.split("\\.");
        if (parts.length != 3) {
            log.error("【Apple服务】identityToken格式错误，不是有效的JWT");
            throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
        }

        // 2. 解析Header（获取kid和alg）
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        JsonNode headerNode = objectMapper.readTree(headerJson);
        String kid = headerNode.has("kid") ? headerNode.get("kid").asText() : null;
        String alg = headerNode.has("alg") ? headerNode.get("alg").asText() : null;

        if (!"RS256".equals(alg)) {
            log.error("【Apple服务】不支持的签名算法：{}，仅支持RS256", alg);
            throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
        }

        // 3. 解析Payload（获取sub、iss、aud、exp等）
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        JsonNode payloadNode = objectMapper.readTree(payloadJson);

        // 4. 验证issuer
        String iss = payloadNode.has("iss") ? payloadNode.get("iss").asText() : null;
        String expectedIssuer = appleProperties.getIssuer();
        if (!expectedIssuer.equals(iss)) {
            log.error("【Apple服务】issuer验证失败：expected={}, actual={}", expectedIssuer, iss);
            throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
        }

        // 5. 验证audience（clientId或serviceId）
        JsonNode audNode = payloadNode.get("aud");
        String expectedAudience = StringUtils.hasText(appleProperties.getServiceId()) 
                ? appleProperties.getServiceId() 
                : appleProperties.getClientId();
        if (!isAudienceMatched(audNode, expectedAudience)) {
            log.error("【Apple服务】audience验证失败：expected={}, actual={}", expectedAudience, audNode);
            throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
        }

        // 6. 验证过期时间
        if (payloadNode.has("exp")) {
            long exp = payloadNode.get("exp").asLong();
            long currentTime = System.currentTimeMillis() / 1000;
            if (exp < currentTime) {
                log.error("【Apple服务】identityToken已过期：exp={}, current={}", exp, currentTime);
                throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
            }
        }

        // 7. 验证签名（通过Apple公钥）
        if (!verifySignature(identityToken, kid)) {
            log.error("【Apple服务】签名验证失败");
            throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
        }

        // 8. 提取sub（用户唯一标识）
        if (!payloadNode.has("sub")) {
            log.error("【Apple服务】payload中缺少sub字段");
            throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
        }

        String sub = payloadNode.get("sub").asText();
        log.info("【Apple服务】验证成功：sub={}, iss={}, aud={}", sub, iss, audNode);
        return sub;
    }

    /**
     * 验证JWT签名
     * 
     * 注意：这是一个简化实现，生产环境需要使用Apple公钥进行完整的RSA签名验证
     * 建议使用 jjwt 或 nimbus-jose-jwt 库来实现完整的签名验证
     * 
     * @param identityToken 完整的JWT token
     * @param kid Key ID（用于从Apple获取对应的公钥）
     * @param header JWT header部分
     * @param payload JWT payload部分
     * @param signature JWT signature部分
     * @return 验证结果
     */
    private boolean verifySignature(String identityToken, String kid) {
        try {
            RSAPublicKey publicKey = getApplePublicKey(kid);
            if (publicKey == null) {
                log.error("【Apple服务】未找到匹配的Apple公钥：kid={}", kid);
                return false;
            }
            Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(identityToken);
            return true;
        } catch (JwtException e) {
            log.error("【Apple服务】签名验证失败：{}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("【Apple服务】签名验证异常：{}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 从Apple获取公钥（用于签名验证）
     * 
     * @param publicKeyUrl Apple公钥URL
     * @return 公钥列表JSON
     */
    private JsonNode fetchApplePublicKeys(String publicKeyUrl) throws Exception {
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
        }

        ResponseEntity<String> response = restTemplate.getForEntity(publicKeyUrl, String.class);
        String responseBody = response.getBody();
        
        if (!StringUtils.hasText(responseBody)) {
            throw new Exception("获取Apple公钥失败：响应为空");
        }

        return objectMapper.readTree(responseBody);
    }

    private RSAPublicKey getApplePublicKey(String kid) throws Exception {
        if (!StringUtils.hasText(kid)) {
            log.error("【Apple服务】identityToken缺少kid");
            return null;
        }
        long now = System.currentTimeMillis();
        if (now < applePublicKeysExpireAt && applePublicKeys.containsKey(kid)) {
            return applePublicKeys.get(kid);
        }
        JsonNode keysResponse = fetchApplePublicKeys(appleProperties.getPublicKeyUrl());
        Map<String, RSAPublicKey> keyMap = new HashMap<>();
        if (keysResponse.has("keys")) {
            for (JsonNode keyNode : keysResponse.get("keys")) {
                String keyKid = keyNode.has("kid") ? keyNode.get("kid").asText() : null;
                String kty = keyNode.has("kty") ? keyNode.get("kty").asText() : null;
                String n = keyNode.has("n") ? keyNode.get("n").asText() : null;
                String e = keyNode.has("e") ? keyNode.get("e").asText() : null;
                if (!"RSA".equals(kty) || !StringUtils.hasText(keyKid) || !StringUtils.hasText(n) || !StringUtils.hasText(e)) {
                    continue;
                }
                RSAPublicKey publicKey = buildRsaPublicKey(n, e);
                keyMap.put(keyKid, publicKey);
            }
        }
        applePublicKeys = keyMap;
        applePublicKeysExpireAt = now + APPLE_PUBLIC_KEYS_CACHE_SECONDS * 1000;
        return applePublicKeys.get(kid);
    }

    private RSAPublicKey buildRsaPublicKey(String n, String e) throws Exception {
        byte[] nBytes = Base64.getUrlDecoder().decode(n);
        byte[] eBytes = Base64.getUrlDecoder().decode(e);
        BigInteger modulus = new BigInteger(1, nBytes);
        BigInteger exponent = new BigInteger(1, eBytes);
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(spec);
    }

    private boolean isAudienceMatched(JsonNode audNode, String expectedAudience) {
        if (audNode == null || !StringUtils.hasText(expectedAudience)) {
            return false;
        }
        if (audNode.isArray()) {
            for (JsonNode node : audNode) {
                if (expectedAudience.equals(node.asText())) {
                    return true;
                }
            }
            return false;
        }
        return expectedAudience.equals(audNode.asText());
    }
}

