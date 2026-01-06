package com.unlimited.sports.globox.payment.service.impl;

import com.unlimited.sports.globox.common.enums.payment.PaymentClientTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaymentsCode;
import com.unlimited.sports.globox.common.utils.AuthContextHolder;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import com.unlimited.sports.globox.payment.service.WechatPayAppService;
import com.unlimited.sports.globox.payment.service.WechatPayJsapiService;
import com.unlimited.sports.globox.payment.service.WechatPayService;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.partnerpayments.app.AppServiceExtension;
import com.wechat.pay.java.service.payments.app.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;

/**
 * 微信支付 服务类
 */
@Slf4j
@Service
public class WechatPayServiceImpl implements WechatPayService {

    @Autowired
    private WechatPayJsapiService wechatPayJsapiService;

    @Autowired
    private WechatPayAppService wechatPayAppService;

    @Autowired
    private NotificationConfig notificationConfig;

    @Override
    public String submit(Payments payments) {
        if (PaymentClientTypeEnum.APP.equals(payments.getClientType())) {
            return wechatPayAppService.submit(payments);
        } else if (PaymentClientTypeEnum.JSAPI.equals(payments.getClientType())) {
            return wechatPayJsapiService.submit(payments);
        } else {
            throw new GloboxApplicationException(PaymentsCode.NOT_SUPPORTED_PAYMENT_CLIENT_TYPE);
        }
    }

    // TODO 移除 SneakyThrows
    @SneakyThrows
    @Override
    public String handleCallback() {
        HttpServletRequest request = AuthContextHolder.getRequest();
        // 获取请求体原内容（此时获取的数据是加密的）
        BufferedReader reader = request.getReader();
        StringBuilder requestBody = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null){
            requestBody.append(line);
        }

        //构造RequestParam （使用回调通知请求的数据, 去获取请求携带的数据）
        RequestParam requestParam = new RequestParam.Builder()
                //微信支付平台证书的证书序列号
                .serialNumber(request.getHeader("Wechatpay-Serial"))
                //随机字符串（签名中的随机数）
                .nonce(request.getHeader("Wechatpay-Nonce"))
                //签名值（应答的微信支付签名）
                .signature(request.getHeader("Wechatpay-Signature"))
                //时间戳（签名中的时间戳）
                .timestamp(request.getHeader("Wechatpay-Timestamp"))
                //请求参数
                .body(requestBody.toString())
                .build();

        NotificationParser parser = new NotificationParser(notificationConfig);

        //支付通知回调 (验签、解密并转换成 Transaction)
        Transaction transaction = parser.parse(requestParam, Transaction.class);
        //支付成功，回调信息
        System.out.println(transaction);
        //微信支付单号
        System.out.println(transaction.getTransactionId());

        return transaction.toString();
    }
}
