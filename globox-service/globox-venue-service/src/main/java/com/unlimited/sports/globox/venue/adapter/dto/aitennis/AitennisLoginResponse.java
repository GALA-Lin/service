package com.unlimited.sports.globox.venue.adapter.dto.aitennis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * aitennis登录响应数据
 */
@Data
public class AitennisLoginResponse {

    /**
     * Token（已包含 "Bearer " 前缀）
     */
    private String token;

    /**
     * Token过期时间戳（秒）
     */
    @JsonProperty("expire_in")
    private Long expireIn;
}
