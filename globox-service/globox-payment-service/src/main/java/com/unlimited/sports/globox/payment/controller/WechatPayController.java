package com.unlimited.sports.globox.payment.controller;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.model.payment.vo.WechatPayNotifyVo;
import com.unlimited.sports.globox.payment.service.WechatPayService;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * 微信支付接入 controller
 */
@Slf4j
@RestController
@RequestMapping("payments/wechat-pay")
public class WechatPayController {

    @Autowired
    private WechatPayService wechatPayService;

    @Autowired
    private NotificationConfig notificationConfig;

    /**
     * 微信异步回调方法
     */
    @RequestMapping("/callback/notify")
    @ResponseBody
    public WechatPayNotifyVo notifyCallback(HttpServletRequest request) {
        return wechatPayService.handleCallback(request, notificationConfig);
    }
}
