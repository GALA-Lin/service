package com.unlimited.sports.globox.payment.controller;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.payment.service.AlipayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付宝支付接入 controller
 */
@Slf4j
@RestController
@RequestMapping("payments/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    /**
     * 支付宝异步回调方法
     */
    @RequestMapping("/callback/notify")
    @ResponseBody
    public String notifyCallback(@RequestParam Map<String, String> paramsMap) {
        log.info("支付宝异步回调触发：{}", paramsMap);
        return alipayService.checkCallback(paramsMap);
    }

}
