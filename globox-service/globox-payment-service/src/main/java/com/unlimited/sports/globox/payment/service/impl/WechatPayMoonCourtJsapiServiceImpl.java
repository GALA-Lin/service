package com.unlimited.sports.globox.payment.service.impl;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaymentsCode;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import com.unlimited.sports.globox.model.payment.vo.SubmitResultVo;
import com.unlimited.sports.globox.payment.prop.WechatPayMoonCourtProperties;
import com.unlimited.sports.globox.payment.service.PaymentsService;
import com.unlimited.sports.globox.payment.service.WechatPayMoonCourtJsapiService;
import com.unlimited.sports.globox.payment.utils.AmountUtils;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * moon court JSAPI 微信支付
 */
@Slf4j
@Service
@Profile("beta")
public class WechatPayMoonCourtJsapiServiceImpl implements WechatPayMoonCourtJsapiService {

    @Lazy
    @Autowired
    private PaymentsService paymentsService;

    @Autowired
    private JsapiServiceExtension moonCourtJsapiService;

    @Autowired
    private WechatPayMoonCourtProperties wechatPayMoonCourtProperties;

    @Autowired
    private RefundService moonCourtRefundService;

    @Autowired
    private JsonUtils jsonUtils;


    /**
     * 提交支付请求到微信JSAPI支付平台。
     *
     * @param payments 包含支付信息的对象，如订单编号、openID等
     * @return 返回微信支付平台返回的预支付交易会话标识，用于后续调起支付
     */
    @Override
    public SubmitResultVo submit(Payments payments) {
        PrepayRequest prepayRequest = new PrepayRequest();
        prepayRequest.setAppid(wechatPayMoonCourtProperties.getAppid());
        prepayRequest.setMchid(wechatPayMoonCourtProperties.getMchid());
        prepayRequest.setDescription(payments.getSubject());
        prepayRequest.setOutTradeNo(payments.getOutTradeNo());
        prepayRequest.setTimeExpire(paymentsService.getPaymentTimeout(payments));
        prepayRequest.setNotifyUrl(wechatPayMoonCourtProperties.getNotifyPaymentUrl());
        Payer payer = new Payer();
        payer.setOpenid(payments.getOpenId());
        prepayRequest.setPayer(payer);
        Amount amount = new Amount();
        amount.setTotal(payments.getTotalAmount()
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact());
        prepayRequest.setAmount(amount);
        PrepayWithRequestPaymentResponse response = moonCourtJsapiService.prepayWithRequestPayment(prepayRequest);
        log.info("prepay 成功：{}", jsonUtils.objectToJson(response));
        return SubmitResultVo.builder()
                .outTradeNo(payments.getOutTradeNo())
                .orderStr(jsonUtils.objectToJson(response))
                .build();
    }


    /**
     * 获取指定支付信息的支付状态。
     *
     * @param payments 包含支付详情的对象，如订单编号、对外业务编号等
     * @return 返回包含支付状态信息的Transaction对象
     */
    @Override
    public Transaction getPaymentStatus(Payments payments) {
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setMchid(wechatPayMoonCourtProperties.getMchid());
        request.setOutTradeNo(payments.getOutTradeNo());
        try {
            return moonCourtJsapiService.queryOrderByOutTradeNo(request);
        } catch (ServiceException e) {
            // API返回失败, 例如ORDER_NOT_EXISTS
            log.error("code={}, message={}}\n", e.getErrorCode(), e.getErrorMessage());
            log.error("reponse body={}\n", e.getResponseBody());
            throw new GloboxApplicationException(PaymentsCode.PAYMENT_INFO_NOT_EXIST.getCode(), e.getErrorMessage());
        }
    }


    /**
     * 取消指定的支付(未支付)。
     *
     * @param payments 包含支付信息的对象，如订单编号、对外业务编号等
     */
    @Override
    public void cancel(Payments payments) {
        CloseOrderRequest closeRequest = new CloseOrderRequest();
        closeRequest.setMchid(wechatPayMoonCourtProperties.getMchid());
        closeRequest.setOutTradeNo(payments.getOutTradeNo());
        moonCourtJsapiService.closeOrder(closeRequest);
    }

    @Override
    public boolean refund(Payments payments, BigDecimal refundAmount, String refundReason) {
        CreateRequest request = new CreateRequest();
        AmountReq amount = new AmountReq();
        amount.setTotal(AmountUtils.toLong(payments.getTotalAmount()));
        amount.setRefund(AmountUtils.toLong(refundAmount));
        amount.setCurrency("CNY");
        request.setAmount(amount);
        request.setNotifyUrl(wechatPayMoonCourtProperties.getNotifyPaymentUrl());
        request.setOutTradeNo(payments.getOutTradeNo());
        request.setOutRefundNo(payments.getOutRequestNo());
        request.setReason(refundReason);
        Refund refund;
        try {
            refund = moonCourtRefundService.create(request);
        } catch (Exception e) {
            log.error("请求微信退款失败：{}", e.getMessage(), e);
            throw new GloboxApplicationException(PaymentsCode.PAYMENT_WECHAT_PAY_FAILED);
        }

        if (refund.getStatus().equals(Status.ABNORMAL)) {
            /* TODO
            描述：
                退款异常，退款到银行发现用户的卡作废或者冻结了，导致原路退款银行卡失败
            解决方案：
                1. 可前往商户平台-交易中心，手动处理此笔退款
                2. 通过发起异常退款接口进行处理（用户需要输入新的银行卡号）
             */
            return true;
        } else if (refund.getStatus().equals(Status.CLOSED)
                || refund.getStatus().equals(Status.PROCESSING)
                || refund.getStatus().equals(Status.SUCCESS)) {
            return true;
        } else {
            log.error("微信退款失败，未知的状态：{}, refund :{}", refund.getStatus(), jsonUtils.objectToJson(refund));
            throw new GloboxApplicationException("微信退款失败 refund: 【%s】".formatted(jsonUtils.objectToJson(refund)));
        }
    }
}
