package com.unlimited.sports.globox.model.payment.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WechatPayNotifyVo {
    private String code;
    private String message;

    public static WechatPayNotifyVo ok() {
        return new WechatPayNotifyVo("SUCCESS", "SUCCESS");
    }

    public static WechatPayNotifyVo fail() {
        return new WechatPayNotifyVo("FAIL", "FAIL");
    }
}