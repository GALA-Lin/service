package com.unlimited.sports.globox.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.unlimited.sports.globox.common.constants.PaymentMQConstants;
import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import com.unlimited.sports.globox.common.enums.payment.PaymentStatusEnum;
import com.unlimited.sports.globox.common.message.payment.PaymentSuccessMessage;
import com.unlimited.sports.globox.common.result.PaymentsCode;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.LocalDateUtils;
import com.unlimited.sports.globox.dubbo.order.OrderDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.PaymentGetOrderResultDto;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import com.unlimited.sports.globox.payment.constants.AlipayPaymentStatusConsts;
import com.unlimited.sports.globox.payment.constants.RedisConsts;
import com.unlimited.sports.globox.payment.prop.AliPayProperties;
import com.unlimited.sports.globox.payment.service.AlipayService;
import com.unlimited.sports.globox.payment.service.PaymentsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
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
    private AliPayProperties aliPayProperties;

    @Autowired
    private PaymentsService paymentsService;

    @Autowired
    private MQService mqService;

    @DubboReference(group = "rpc")
    private OrderDubboService orderDubboService;

    @Override
    public String submit(Long orderNo) {
        PaymentGetOrderResultDto resultDto = orderDubboService.paymentGetOrders(orderNo);

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

        // 1. 验签
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
            log.warn("支付宝验签失败: {}", paramsMap);
            return "failure";
        }

        // 2. 基础参数校验
        String outTradeNo = paramsMap.get("out_trade_no");
        String totalAmount = paramsMap.get("total_amount");
        String appId = paramsMap.get("app_id");
        String tradeStatus = paramsMap.get("trade_status");

        if (ObjectUtils.isEmpty(outTradeNo) || ObjectUtils.isEmpty(totalAmount) || ObjectUtils.isEmpty(appId)) {
            return "failure";
        }

        Payments payments = paymentsService.getPaymentInfo(outTradeNo, PaymentTypeEnum.ALIPAY);
        Assert.isNotEmpty(payments, PaymentsCode.PAYMENT_INFO_NOT_EXIST);

        if (!aliPayProperties.getAppId().equals(appId)) {
            log.warn("支付宝 appId 不匹配, outTradeNo={}", outTradeNo);
            return "failure";
        }

        if (payments.getTotalAmount().compareTo(new BigDecimal(totalAmount)) != 0) {
            log.warn("支付宝金额不匹配, outTradeNo={}", outTradeNo);
            return "failure";
        }

        // 3. 已支付直接返回 success（重复回调）
        if (!PaymentStatusEnum.UNPAID.equals(payments.getPaymentStatus())) {
            return "success";
        }

        // 4. 只处理成功状态

        if (!AlipayPaymentStatusConsts.SUCCESS.equals(tradeStatus)) {
            // 非成功状态，记录即可，不入账
            return "success";
        }

        // 5. Redis 幂等（削峰用）
        String redisKey = RedisConsts.ALIPAY_CALLBACK_LOCK + outTradeNo;

        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", 1, TimeUnit.DAYS);

        // Redis 拿不到锁：说明有并发回调正在处理
        if (Boolean.FALSE.equals(locked)) {
            return "success";
        }

        try {
            // 6. DB 条件更新（最终幂等）
            boolean firstSuccess = paymentsService.updatePaymentSuccess(
                    payments.getId(), paramsMap);

            // 7. 只有第一次成功才发 MQ
            if (firstSuccess) {
                String tradeNo = paramsMap.get("trade_no");
                LocalDateTime paymentAt = LocalDateUtils.from(paramsMap.get("gmt_payment"));
                PaymentSuccessMessage message = PaymentSuccessMessage.builder()
                        .orderNo(payments.getOrderNo())
                        .tradeNo(tradeNo)
                        .paymentAt(paymentAt)
                        .totalAmount(new BigDecimal(totalAmount))
                        .paymentType(PaymentTypeEnum.ALIPAY)
                        .build();

                mqService.send(
                        PaymentMQConstants.EXCHANGE_TOPIC_PAYMENT_SUCCESS,
                        PaymentMQConstants.ROUTING_PAYMENT_SUCCESS,
                        message);
            }

            return "success";
        } catch (Exception e) {
            // DB/MQ 出异常，删除 Redis Key，允许支付宝重试
            redisTemplate.delete(redisKey);
            log.error("支付宝回调处理失败, outTradeNo={}", outTradeNo, e);
            return "failure";
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
