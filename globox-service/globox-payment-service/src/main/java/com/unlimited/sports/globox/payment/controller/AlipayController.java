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

    @Autowired
    private JsonUtils jsonUtils;


    @PostMapping("/submit/{orderNo}")
    public R<String> submit(@PathVariable("orderNo") Long orderNo) {
        return R.ok(alipayService.submit(orderNo));
    }


    /**
     * 支付成功后的接口调用
     * 重定向到网页中
     */
    @RequestMapping("/callback/return")
    public String callback() {
        log.info("支付成功回调触发");
        return "https://www.baidu.com";
    }


    /**
     * 支付宝异步回调方法
     */
    @RequestMapping("/callback/notify")
    @ResponseBody
    public String notifyCallback(@RequestParam Map<String, String> paramsMap) {
        // TODO ETA 2026/01/04 测试使用
        log.info("异步回调触发：{}", jsonUtils.objectToJson(paramsMap));
        return alipayService.checkCallback(paramsMap);
    }

}
