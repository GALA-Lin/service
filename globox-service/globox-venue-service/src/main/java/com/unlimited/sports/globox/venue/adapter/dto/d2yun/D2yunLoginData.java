package com.unlimited.sports.globox.venue.adapter.dto.d2yun;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * D2yun登录响应数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class D2yunLoginData {

    /**
     * Token
     */
    private String token;

    /**
     * 商户ID
     */
    @JsonProperty("business_id")
    private Long businessId;

    /**
     * 场馆ID
     */
    @JsonProperty("stadium_id")
    private Long stadiumId;

    /**
     * refresh token
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * 过期时间（秒）
     */
    private Long expiresin;
}
