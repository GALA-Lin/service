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
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.common.utils.LocalDateUtils;
import com.unlimited.sports.globox.dubbo.order.OrderForPaymentDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.PaymentGetOrderResultDto;
import com.unlimited.sports.globox.payment.constants.AlipayPaymentStatusConsts;
import com.unlimited.sports.globox.payment.constants.RedisConsts;
import com.unlimited.sports.globox.payment.prop.AlipayProperties;
import com.unlimited.sports.globox.payment.service.AlipayService;
import com.unlimited.sports.globox.payment.service.PaymentsService;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${payment.normal.timeout}")
    private Integer normalTimeout;

    @Value("${payment.activity.timeout}")
    private Integer activityTimeout;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private AlipayProperties aliPayProperties;

    @Autowired
    private PaymentsService paymentsService;

    @Autowired
    private MQService mqService;

    @Autowired
    private JsonUtils jsonUtils;

    @DubboReference(group = "rpc")
    private OrderForPaymentDubboService orderService;

    @Override
    public String submit(Long orderNo) {
        RpcResult<PaymentGetOrderResultDto> rpcResult = orderService.paymentGetOrders(orderNo);
        Assert.rpcResultOk(rpcResult);
        PaymentGetOrderResultDto resultDto = rpcResult.getData();

        // 1) 插入支付记录
        boolean isInsert = paymentsService.savePayments(resultDto, PaymentTypeEnum.ALIPAY);
        Assert.isTrue(isInsert, PaymentsCode.PAYMENT_INFO_CREATE_FAILED);

        // 2) 对接支付宝

        // 移动支付 request
        AlipayTradeAppPayRequest alipayRequest = new AlipayTradeAppPayRequest();

        // 设置同步回调地址
        alipayRequest.setReturnUrl(aliPayProperties.getReturnPaymentUrl());
        // 在公共参数中设置回跳和通知地址
        // 设置异步回调地址
        alipayRequest.setNotifyUrl(aliPayProperties.getNotifyPaymentUrl());

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", resultDto.getOutTradeNo());
        // 商品价格在这里写死
        bizContent.put("total_amount", resultDto.getTotalAmount());
        // 商品名称
        bizContent.put("subject", resultDto.getSubject());
        // TODO 销售产品码，销售产品码，商家和支付宝签约的产品码
        bizContent.put("product_code", "QUICK_MSECURITY_PAY");

        // 设置超时时间 - 绝对时间
        bizContent.put("time_expire", this.getTimeOut(resultDto.isActivity()));

        // 填充业务参数
        alipayRequest.setBizContent(bizContent.toJSONString());
        String orderStr = "";
        try {
            // 调用SDK生成表单
            AlipayTradeAppPayResponse response = alipayClient.sdkExecute(alipayRequest);
            orderStr = response.getBody();
        } catch (AlipayApiException e) {
            log.error("请求支付宝失败：{}", e.getMessage(), e);
        }

        return orderStr;
    }

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
            return "failure";
        }

        if (!signVerified) {
            log.warn("支付宝验签失败: {}", jsonUtils.objectToJson(paramsMap));
            return "failure";
        }

        // 2) 基础参数
        String outTradeNo = paramsMap.get("out_trade_no");
        String totalAmountStr = paramsMap.get("total_amount");
        String appId = paramsMap.get("app_id");
        String tradeStatus = paramsMap.get("trade_status");
        String notifyId = paramsMap.get("notify_id");     // 用于回调级幂等
        String outBizNo = paramsMap.get("out_biz_no");     // 退款请求号（你要落到 outRequestNo）

        if (ObjectUtils.isEmpty(outTradeNo) || ObjectUtils.isEmpty(totalAmountStr) || ObjectUtils.isEmpty(appId)) {
            return "failure";
        }

        Payments payments = paymentsService.getPaymentInfoByType(outTradeNo, PaymentTypeEnum.ALIPAY);
        Assert.isNotEmpty(payments, PaymentsCode.PAYMENT_INFO_NOT_EXIST);

        if (!aliPayProperties.getAppId().equals(appId)) {
            log.warn("支付宝 appId 不匹配, outTradeNo={}, appId={}", outTradeNo, appId);
            return "failure";
        }

        BigDecimal totalAmount = new BigDecimal(totalAmountStr);
        if (payments.getTotalAmount().compareTo(totalAmount) != 0) {
            log.warn("支付宝金额不匹配, outTradeNo={}, db={}, cb={}",
                    outTradeNo, payments.getTotalAmount(), totalAmountStr);
            return "failure";
        }

        // 3) Redis 削峰锁：必须按“回调事件”粒度，而不是按 outTradeNo 粒度
        // 优先用 notify_id；退款也可附带 out_biz_no 做增强
        String lockSuffix = !ObjectUtils.isEmpty(notifyId) ? notifyId : (outBizNo != null ? outBizNo : tradeStatus);
        String redisKey = RedisConsts.ALIPAY_CALLBACK_LOCK + outTradeNo + ":" + lockSuffix;

        Boolean locked = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", 30, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(locked)) {
            return "success";
        }

        try {
            // 4) 区分“退款回调” vs “支付状态回调”
            if (isRefundCallback(paramsMap)) {
                return handleRefundCallback(payments, paramsMap);
            }

            // 5) 支付成功/关闭
            if (AlipayPaymentStatusConsts.TRADE_SUCCESS.equals(tradeStatus)) {
                return handlePaySuccess(payments, paramsMap);
            } else if (AlipayPaymentStatusConsts.TRADE_CLOSED.equals(tradeStatus)) {
                return handleTradeClosed(payments, paramsMap);
            } else {
                paymentsService.appendCallback(payments.getId(), paramsMap);
                log.warn("未处理的状态：{}", jsonUtils.objectToJson(paramsMap));
                return "success";
            }

        } catch (Exception e) {
            // 出异常允许重试
            redisTemplate.delete(redisKey);
            log.error("支付宝回调处理失败, outTradeNo={}", outTradeNo, e);
            return "failure";
        }
    }


    private String handleRefundCallback(Payments payments, Map<String, String> paramsMap) {

        // 退款回调也把回调信息记录一下
        paymentsService.appendCallback(payments.getId(), paramsMap);

        BigDecimal totalAmount = new BigDecimal(paramsMap.get("total_amount"));
        BigDecimal refundFee = new BigDecimal(paramsMap.get("refund_fee"));
        String tradeStatus = paramsMap.get("trade_status");
        String outBizNo = paramsMap.get("out_biz_no"); // 退款请求号（落库到 outRequestNo）
        LocalDateTime refundAt;
        try {
             refundAt = LocalDateUtils.from(paramsMap.get("gmt_refund"));
        } catch (DateTimeParseException e) {
            refundAt = LocalDateUtils.from(paramsMap.get("gmt_refund"), LocalDateUtils.DATETIME_PATTERN_4);
        }

        // 兜底：如果还没标记 PAID，先尝试补一次 PAID（条件更新）
        // 不管返回值，用条件更新保证不会把已退款覆盖
        paymentsService.tryMarkPaidIfUnpaid(payments.getId(), paramsMap);

        boolean isFullRefund = refundFee.compareTo(totalAmount) >= 0
                || (AlipayPaymentStatusConsts.TRADE_CLOSED.equals(tradeStatus) && refundFee.compareTo(BigDecimal.ZERO) > 0);

        if (isFullRefund) {
            boolean first = paymentsService.updateRefunded(payments.getId(), outBizNo, refundFee, refundAt, paramsMap);
            if (first) {
                PaymentRefundMessage refundMessage = PaymentRefundMessage.builder()
                        .orderNo(payments.getOrderNo())
                        .outRequestNo(payments.getOutRequestNo())
                        .build();

                mqService.send(
                        PaymentMQConstants.EXCHANGE_TOPIC_PAYMENT_REFUND_SUCCESS,
                        PaymentMQConstants.ROUTING_PAYMENT_REFUND_SUCCESS,
                        refundMessage);
            }
            return "success";
        }

        // 部分退款
        boolean first = paymentsService.updatePartiallyRefunded(payments.getId(), outBizNo, refundFee, refundAt, paramsMap);
        if (first) {
            PaymentRefundMessage refundMessage = PaymentRefundMessage.builder()
                    .orderNo(payments.getOrderNo())
                    .outRequestNo(payments.getOutRequestNo())
                    .build();
            mqService.send(
                    PaymentMQConstants.EXCHANGE_TOPIC_PAYMENT_REFUND_SUCCESS,
                    PaymentMQConstants.ROUTING_PAYMENT_REFUND_SUCCESS,
                    refundMessage);
        }
        return "success";
    }


    private String handleTradeClosed(Payments payments, Map<String, String> paramsMap) {

        PaymentStatusEnum status = payments.getPaymentStatus();

        // 已支付/退款/部分退款：TRADE_CLOSED 不覆盖支付事实，只记录
        if (PaymentStatusEnum.PAID.equals(status)
                || PaymentStatusEnum.PARTIALLY_REFUNDED.equals(status)
                || PaymentStatusEnum.REFUND.equals(status)) {
            paymentsService.appendCallback(payments.getId(), paramsMap);
            return "success";
        }

        // 仅 UNPAID -> CLOSED
        boolean firstClosed = paymentsService.updatePaymentClosed(payments.getId(), paramsMap);

        if (firstClosed) {
            PaymentCancelMessage cancelMessage = PaymentCancelMessage.builder()
                    .orderNo(payments.getOrderNo())
                    .build();

            mqService.send(
                    PaymentMQConstants.EXCHANGE_TOPIC_PAYMENT_CANCEL,
                    PaymentMQConstants.ROUTING_PAYMENT_CANCEL,
                    cancelMessage
            );
        }

        return "success";
    }

    private String handlePaySuccess(Payments payments, Map<String, String> paramsMap) {

        // 已经处于退款/部分退款/已支付，也可以只记录回调
        if (PaymentStatusEnum.PAID.equals(payments.getPaymentStatus())
                || PaymentStatusEnum.PARTIALLY_REFUNDED.equals(payments.getPaymentStatus())
                || PaymentStatusEnum.REFUND.equals(payments.getPaymentStatus())) {
            paymentsService.appendCallback(payments.getId(), paramsMap);
            return "success";
        }

        boolean firstSuccess = paymentsService.updatePaymentSuccess(payments.getId(), paramsMap);

        if (firstSuccess) {
            String totalAmountStr = paramsMap.get("total_amount");
            String tradeNo = paramsMap.get("trade_no");
            LocalDateTime paymentAt= LocalDateUtils.from(paramsMap.get("gmt_payment"));

            PaymentSuccessMessage message = PaymentSuccessMessage.builder()
                    .orderNo(payments.getOrderNo())
                    .tradeNo(tradeNo)
                    .paymentAt(paymentAt)
                    .totalAmount(new BigDecimal(totalAmountStr))
                    .paymentType(PaymentTypeEnum.ALIPAY)
                    .build();

            mqService.send(
                    PaymentMQConstants.EXCHANGE_TOPIC_PAYMENT_SUCCESS,
                    PaymentMQConstants.ROUTING_PAYMENT_SUCCESS,
                    message
            );
        }
        return "success";
    }

    private boolean isRefundCallback(Map<String, String> paramsMap) {
        String refundFee = paramsMap.get("refund_fee");
        if (ObjectUtils.isEmpty(paramsMap.get("refund_fee")) || ObjectUtils.isEmpty(paramsMap.get("gmt_refund"))) {
            return false;
        }
        try {
            return new BigDecimal(refundFee).compareTo(BigDecimal.ZERO) > 0;
        } catch (Exception e) {
            return false;
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refund(UserRefundMessage message) {
        // 根据 orderId 查询支付信息
        Payments payments = paymentsService.getPaymentInfoByOutTradeNo(message.getOutTradeNo());

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
            paymentsService.updatePayment(payments);
            return true;
        } else {
            return false;
        }
    }


    /**
     * 获取超时时间
     */
    private String getTimeOut(boolean activity) {
        Calendar calendar = Calendar.getInstance();
        if (activity) {
            calendar.add(Calendar.MINUTE, activityTimeout);
        } else {
            calendar.add(Calendar.MINUTE, normalTimeout);
        }

        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime());
    }
}
