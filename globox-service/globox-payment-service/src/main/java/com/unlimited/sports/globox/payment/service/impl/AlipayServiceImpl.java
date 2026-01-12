package com.unlimited.sports.globox.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.diagnosis.DiagnosisUtils;
import com.alipay.api.domain.AlipayTradeCloseModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.unlimited.sports.globox.common.constants.PaymentMQConstants;
import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import com.unlimited.sports.globox.common.enums.payment.PaymentStatusEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.message.payment.PaymentSuccessMessage;
import com.unlimited.sports.globox.common.result.PaymentsCode;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.common.utils.LocalDateUtils;
import com.unlimited.sports.globox.model.payment.vo.GetPaymentStatusResultVo;
import com.unlimited.sports.globox.model.payment.vo.SubmitResultVo;
import com.unlimited.sports.globox.payment.constants.AlipayPaymentStatusConsts;
import com.unlimited.sports.globox.payment.prop.AlipayProperties;
import com.unlimited.sports.globox.payment.service.AlipayService;
import com.unlimited.sports.globox.payment.service.PaymentsService;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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


    /**
     * 处理用户的退款请求。
     *
     * @param payments     支付信息
     * @param refundAmount 本次退款金额
     * @param refundReason 本次退款原因
     * @return 如果退款请求处理成功，则返回true；否则返回false
     */
    @Override
    public boolean refund(Payments payments, BigDecimal refundAmount, String refundReason) {

        // 退款申请
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payments.getOutTradeNo());
        bizContent.put("out_request_no", payments.getOutRequestNo());
        bizContent.put("refund_amount", refundAmount);
        bizContent.put("refund_result", refundReason);

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
    public SubmitResultVo submit(Payments payments) {
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
//      销售产品码，销售产品码，应该是商家和支付宝签约的产品码
//        bizContent.put("product_code", "QUICK_MSECURITY_PAY");

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

        return SubmitResultVo.builder()
                .orderStr(orderStr)
                .outTradeNo(payments.getOutTradeNo())
                .build();
    }


    /**
     * 查询指定订单号的支付状态。
     *
     * @param outTradeNo 商家订单号，用于查询该订单的支付状态
     * @return GetPaymentStatusVo 包含支付状态信息的对象。如果支付成功、关闭或完成，则分别设置相应的支付状态；如果遇到未知支付状态，则抛出异常。
     * @throws GloboxApplicationException 当请求支付宝失败或者支付状态未知时抛出此异常。
     */
    @Override
    public GetPaymentStatusResultVo getPaymentStatus(String outTradeNo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", outTradeNo);
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response;
        try {
            response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                throw new GloboxApplicationException(PaymentsCode.PAYMENT_REQUEST_PLATFORM_ERROR);
            }

            String tradeStatus = response.getTradeStatus();

            GetPaymentStatusResultVo paymentStatusVo = GetPaymentStatusResultVo.builder()
                    .outTradeNo(outTradeNo)
                    .build();

            if(AlipayPaymentStatusConsts.WAIT_BUYER_PAY.equals(tradeStatus)){
                paymentStatusVo.setPaymentStatus(PaymentStatusEnum.UNPAID);
            }else if (AlipayPaymentStatusConsts.TRADE_SUCCESS.equals(tradeStatus)) {
                paymentStatusVo.setPaymentStatus(PaymentStatusEnum.PAID);
            } else if (AlipayPaymentStatusConsts.TRADE_CLOSED.equals(tradeStatus)) {
                paymentStatusVo.setPaymentStatus(PaymentStatusEnum.CLOSED);
            } else if (AlipayPaymentStatusConsts.TRADE_FINISHED.equals(tradeStatus)){
                paymentStatusVo.setPaymentStatus(PaymentStatusEnum.FINISH);
            } else {
                log.error("未知的支付状态:{}", tradeStatus);
                throw new GloboxApplicationException(PaymentsCode.PAYMENT_STATUS_UNKNOW);
            }
            return paymentStatusVo;
        } catch (AlipayApiException e) {
            log.error("请求支付宝失败：{}", e.getMessage(), e);
            throw new GloboxApplicationException(PaymentsCode.PAYMENT_ALIPAY_FAILED);
        }
    }


    @Override
    public void cancel(Payments payments) {
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        AlipayTradeCloseModel model = new AlipayTradeCloseModel();
        // 设置订单支付时传入的商户订单号
        model.setOutTradeNo(payments.getOutTradeNo());
        request.setBizModel(model);
        try {
            alipayClient.execute(request);
        } catch (AlipayApiException e) {
            log.error("请求支付宝失败：{}", e.getMessage(), e);
            throw new GloboxApplicationException(PaymentsCode.PAYMENT_ALIPAY_FAILED);
        }
    }
}
