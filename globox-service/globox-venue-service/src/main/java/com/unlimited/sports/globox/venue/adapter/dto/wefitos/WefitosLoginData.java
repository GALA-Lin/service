package com.unlimited.sports.globox.venue.adapter.dto.wefitos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wefitos登录响应数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WefitosLoginData {

    /**
     * 随机数
     */
    private String rnd;

    /**
     * 是否开启软件狗
     */
    @JsonProperty("isOpenSoftDog")
    private String isOpenSoftDog;

    /**
     * 用户信息
     */
    private Object user;
}
