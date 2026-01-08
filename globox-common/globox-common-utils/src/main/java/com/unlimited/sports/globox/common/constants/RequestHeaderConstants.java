package com.unlimited.sports.globox.common.constants;

/**
 * 请求头常量
 *
 * @author Wreckloud
 * @since 2025/12/22
 */
public final class RequestHeaderConstants {

    private RequestHeaderConstants() {
    }

    /**
     * 客户端类型请求头: app/jsapi/merchant/third-party-jsapi
     */
    public static final String HEADER_CLIENT_TYPE = "X-Client-Type";

    /**
     * 用户ID请求头
     */
    public static final String HEADER_USER_ID = "X-User-Id";

    /**
     * 用户角色请求头
     */
    public static final String HEADER_USER_ROLE = "X-User-Role";

    /**
     * 商家ID请求头
     */
    public static final String HEADER_MERCHANT_ID = "X-Merchant-Id";

    /**
     * 商家角色请求头
     */
    public static final String HEADER_MERCHANT_ROLE = "X-Merchant-Role";

    /**
     * 第三方jsapi用户ID请求头
     */
    public static final String HEADER_THIRD_PARTY_ID = "X-Third-Party-Id";

    /**
     * 第三方jsapi用户角色请求头
     */
    public static final String HEADER_THIRD_PARTY_ROLE = "X-Third-Party-Role";

    /**
     * 第三方jsapi用户openid请求头
     */
    public static final String HEADER_THIRD_PARTY_OPENID = "X-Third-Party-Openid";
}


