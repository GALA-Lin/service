package com.unlimited.sports.globox.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.diagnosis.DiagnosisUtils;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.unlimited.sports.globox.common.constants.PaymentMQConstants;
import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import com.unlimited.sports.globox.common.enums.payment.PaymentStatusEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.message.order.UserRefundMessage;
import com.unlimited.sports.globox.common.message.payment.PaymentCancelMessage;
import com.unlimited.sports.globox.common.message.payment.PaymentRefundMessage;
import com.unlimited.sports.globox.common.message.payment.PaymentSuccessMessage;
import com.unlimited.sports.globox.common.result.PaymentsCode;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.common.utils.LocalDateUtils;
import com.unlimited.sports.globox.payment.constants.AlipayPaymentStatusConsts;
import com.unlimited.sports.globox.payment.constants.RedisConsts;
import com.unlimited.sports.globox.payment.prop.AlipayProperties;
import com.unlimited.sports.globox.payment.service.AlipayService;
import com.unlimited.sports.globox.payment.service.PaymentsService;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 支付宝支付接入服务类
 */
@Slf4j
@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private AlipayProperties aliPayProperties;

    @Lazy
    @Autowired
    private PaymentsService paymentsService;

    @Autowired
    private MQService mqService;

    @Autowired
    private JsonUtils jsonUtils;


    @Override
    public String checkCallback(Map<String, String> paramsMap) {

        // 1) 验签
        boolean signVerified;
        try {
            signVerified = AlipaySignature.rsaCheckV1(
                    paramsMap,
                    aliPayProperties.getAlipayPublicKey(),
                    aliPayProperties.getCharset(),
                    aliPayProperties.getSignType());
        } catch (AlipayApiException e) {
            log.error("支付宝验签异常", e);
            return "fail";
        }

        if (!signVerified) {
            log.warn("支付宝验签失败: {}", jsonUtils.objectToJson(paramsMap));
            return "fail";
        }

        // 2) 基础参数
        String outTradeNo = paramsMap.get("out_trade_no");
        String totalAmountStr = paramsMap.get("total_amount");
        String appId = paramsMap.get("app_id");
        String tradeStatus = paramsMap.get("trade_status");
        // 用于回调级幂等
        String notifyId = paramsMap.get("notify_id");

        if (ObjectUtils.isEmpty(outTradeNo) || ObjectUtils.isEmpty(totalAmountStr) || ObjectUtils.isEmpty(appId)) {
            return "fail";
        }

        // 3) 只处理交易成功回调
        if (!AlipayPaymentStatusConsts.TRADE_SUCCESS.equals(tradeStatus)) {
            return "success";
        }


        // 忽略退款回调
        String refundCallbackFlag = paramsMap.getOrDefault("gmt_refund", null);
        if (!ObjectUtils.isEmpty(refundCallbackFlag)) {
            return "success";
        }

        // 4) 核对金额和参数
        Payments payments = paymentsService.getPaymentByOutTradeNo(outTradeNo);
        // 不存在的支付信息
        if (payments == null) {
            return "success";
        }

        // appid 不匹配
        if (!aliPayProperties.getAppId().equals(appId)) {
            log.warn("支付宝 appId 不匹配, outTradeNo={}, appId={}", outTradeNo, appId);
            return "success";
        }

        // 支付金额不匹配
        BigDecimal totalAmount = new BigDecimal(totalAmountStr);
        if (payments.getTotalAmount().compareTo(totalAmount) != 0) {
            log.warn("支付宝金额不匹配, outTradeNo={}, db={}, cb={}",
                    outTradeNo, payments.getTotalAmount(), totalAmountStr);
            return "fail";
        }

        // 已经处理过的支付信息
        if (!PaymentStatusEnum.UNPAID.equals(payments.getPaymentStatus())) {
            return "success";
        }

        // 5) 添加标识
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(notifyId, notifyId, 1461, TimeUnit.MINUTES);
        if (Boolean.TRUE.equals(flag)) {
            // 6) 设置支付状态信息
            String tradeNo = paramsMap.get("trade_no");
            LocalDateTime paymentAt = LocalDateUtils.from(paramsMap.get("gmt_payment"));
            LocalDateTime callbackAt = LocalDateUtils.from(paramsMap.get("notify_time"));
            String callbackContent = jsonUtils.objectToJson(paramsMap);

            payments.setTradeNo(tradeNo);
            payments.setPaymentAt(paymentAt);
            payments.setCallbackAt(callbackAt);
            payments.setCallbackContent(callbackContent);
            int cnt = paymentsService.updatePayment(payments);
            if (cnt > 0) {
                PaymentSuccessMessage message = PaymentSuccessMessage.builder()
                        .orderNo(payments.getOrderNo())
                        .tradeNo(tradeNo)
                        .outTradeNo(outTradeNo)
                        .paymentAt(paymentAt)
                        .totalAmount(new BigDecimal(totalAmountStr))
                        .paymentType(PaymentTypeEnum.ALIPAY)
                        .build();

                mqService.send(
                        PaymentMQConstants.EXCHANGE_TOPIC_PAYMENT_SUCCESS,
                        PaymentMQConstants.ROUTING_PAYMENT_SUCCESS,
                        message);
                return "success";
            } else {
                return "fail";
            }
        }
        return "fail";
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refund(UserRefundMessage message) {
        // 根据 orderId 查询支付信息
        Payments payments = paymentsService.getPaymentByOutTradeNo(message.getOutTradeNo());

        // 判断
        if (payments == null) {
            return false;
        }

        // 退款申请
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", message.getOutTradeNo());
        bizContent.put("out_request_no", message.getOutRequestNo());
        bizContent.put("refund_amount", message.getRefundAmount());
        bizContent.put("refund_result", message.getRefundReason());

        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
            String diagnosisUrl = DiagnosisUtils.getDiagnosisUrl(response);
            System.out.println(diagnosisUrl);
        } catch (AlipayApiException e) {
            log.error("向支付宝申请退款失败:{}", e.getMessage(), e);
            throw new GloboxApplicationException(e);
        }
        if (response.isSuccess()) {
            // 修改支付记录表
            payments.setPaymentStatus(PaymentStatusEnum.REFUND);
            payments.setOutRequestNo(message.getOutRequestNo());
            BigDecimal refundAmount = payments.getRefundAmount();
            BigDecimal currentRefundAmount = refundAmount.add(message.getRefundAmount());
            payments.setRefundAmount(currentRefundAmount);
            paymentsService.updatePayment(payments);

            return true;
        } else {
            return false;
        }
    }


    /**
     * 支付宝提交支付
     *
     * @param payments 支付信息
     * @return orderStr
     */
    @Override
    public String submit(Payments payments) {
        // 移动支付 request
        AlipayTradeAppPayRequest alipayRequest = new AlipayTradeAppPayRequest();

        // 设置同步回调地址
        alipayRequest.setReturnUrl(aliPayProperties.getReturnPaymentUrl());
        // 在公共参数中设置回跳和通知地址
        // 设置异步回调地址
        alipayRequest.setNotifyUrl(aliPayProperties.getNotifyPaymentUrl());

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payments.getOutTradeNo());
        // 商品价格在这里写死
        bizContent.put("total_amount", payments.getTotalAmount());
        // 商品名称
        bizContent.put("subject", payments.getSubject());
        // TODO 修改销售产品码，销售产品码，应该是商家和支付宝签约的产品码
        bizContent.put("product_code", "QUICK_MSECURITY_PAY");

        // 设置超时时间 - 绝对时间
        bizContent.put("time_expire", paymentsService.getPaymentTimeout(payments));

        // 填充业务参数
        alipayRequest.setBizContent(bizContent.toJSONString());
        String orderStr = "";
        try {
            // 调用SDK生成表单
            AlipayTradeAppPayResponse response = alipayClient.sdkExecute(alipayRequest);
            orderStr = response.getBody();
        } catch (AlipayApiException e) {
            log.error("请求支付宝失败：{}", e.getMessage(), e);
            throw new GloboxApplicationException(PaymentsCode.PAYMENT_ALIPAY_FAILED);
        }

        return orderStr;
    }
}
