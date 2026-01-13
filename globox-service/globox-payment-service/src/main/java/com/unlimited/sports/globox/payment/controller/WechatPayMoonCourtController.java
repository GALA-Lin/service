package com.unlimited.sports.globox.payment.controller;

import com.unlimited.sports.globox.model.payment.vo.WechatPayNotifyVo;
import com.unlimited.sports.globox.payment.service.WechatPayService;
import com.wechat.pay.java.core.notification.NotificationConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 微信支付接入 controller
 */
@Slf4j
@Profile("beta")
@RestController
@RequestMapping("payments/wechat-pay/mooncourt")
public class WechatPayMoonCourtController {

    @Autowired
    private WechatPayService wechatPayService;

    @Autowired
    private NotificationConfig moonCourtNotificationConfig;

    /**
     * 微信异步回调方法
     */
    @RequestMapping("/callback/notify")
    @ResponseBody
    public WechatPayNotifyVo notifyCallback(HttpServletRequest request) {
        return wechatPayService.handleCallback(request, moonCourtNotificationConfig);
    }
}
