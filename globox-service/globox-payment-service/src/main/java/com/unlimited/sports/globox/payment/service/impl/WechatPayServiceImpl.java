package com.unlimited.sports.globox.payment.service.impl;

import com.unlimited.sports.globox.common.constants.PaymentMQConstants;
import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import com.unlimited.sports.globox.common.enums.order.SellerTypeEnum;
import com.unlimited.sports.globox.common.enums.payment.PaymentStatusEnum;
import com.unlimited.sports.globox.common.enums.payment.ProfitSharingStatusEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.message.payment.PaymentSuccessMessage;
import com.unlimited.sports.globox.common.result.ApplicationCode;
import com.unlimited.sports.globox.common.result.PaymentsCode;
import com.unlimited.sports.globox.common.result.ResultCode;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.common.utils.LocalDateUtils;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.CoachInfoForProfitSharing;
import com.unlimited.sports.globox.model.payment.entity.PaymentProfitSharing;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import com.unlimited.sports.globox.model.payment.vo.GetPaymentStatusResultVo;
import com.unlimited.sports.globox.model.payment.vo.SubmitResultVo;
import com.unlimited.sports.globox.model.payment.vo.WechatPayNotifyVo;
import com.unlimited.sports.globox.payment.prop.ProfitSharingProperties;
import com.unlimited.sports.globox.payment.prop.WechatPayProperties;
import com.unlimited.sports.globox.payment.service.*;
import com.unlimited.sports.globox.payment.utils.AmountUtils;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.model.Transaction.*;
import com.wechat.pay.java.service.profitsharing.model.OrderStatus;
import com.wechat.pay.java.service.profitsharing.model.OrdersEntity;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.wechat.pay.java.core.http.Constant.*;

/**
 * 微信支付 服务类
 */
@Slf4j
@Service
public class WechatPayServiceImpl implements WechatPayService {

    @Autowired
    private WechatPayMoonCourtJsapiService wechatPayMoonCourtJsapiService;

    @Autowired
    private WechatPayJsapiService wechatPayJsapiService;

    @Autowired
    private WechatPayAppService wechatPayAppService;

    @Autowired
    private ProfitSharingProperties profitSharingProperties;

    @Autowired
    private RefundService refundService;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Lazy
    @Autowired
    private PaymentsService paymentsService;

    @Autowired
    private MQService mqService;

    @Autowired
    private JsonUtils jsonUtils;

    @DubboReference(group = "rpc")
    private UserDubboService userDubboService;


    /**
     * 提交支付请求到微信支付平台，分派给 appService 或 jsapiService
     *
     * @param payments 包含支付信息的对象，如订单编号、openID 等
     * @return 返回微信支付平台返回的预支付交易会话标识，用于后续调起支付
     */
    @Override
    public SubmitResultVo submit(Payments payments) {
        if (ClientType.APP.equals(payments.getClientType())) {
            return wechatPayAppService.submit(payments);
        } else if (ClientType.JSAPI.equals(payments.getClientType())) {
            return wechatPayJsapiService.submit(payments);
        } else if (ClientType.THIRD_PARTY_JSAPI.equals(payments.getClientType())) {
            return wechatPayMoonCourtJsapiService.submit(payments);
        } else {
            throw new GloboxApplicationException(PaymentsCode.NOT_SUPPORTED_PAYMENT_CLIENT_TYPE);
        }
    }

    @Override
    public WechatPayNotifyVo handleCallback(HttpServletRequest request, NotificationConfig notificationConfig) {
        LocalDateTime callbackAt = LocalDateTime.now();

        String notify;
        try (ServletInputStream inputStream = request.getInputStream();
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            // 读取请求体
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            notify = sb.toString();
        } catch (IOException e) {
            log.error("[微信支付通知回调] 解析付款通知出错：{}", e.getMessage(), e);
            return WechatPayNotifyVo.fail();
        }
        Map<String, Object> bodyMap = jsonUtils.jsonToMap(notify);
        log.info("[微信支付通知回调] 异步回调触发： {}", bodyMap);
        RequestParam requestParam = buildByHeaderRequestParam(request, notify);
        // 验证签名并解析请求体
        NotificationParser notificationParser = new NotificationParser(notificationConfig);
        // 调用 NotificationParser.parse() 验签、解密并将 JSON 转换成具体的通知回调对象。如果验签失败，SDK 会抛出 ValidationException。
        Transaction transaction;
        try {
            /*
             * 常用的通知回调调对象类型有：
             * 支付 Transaction
             * 退款 RefundNotification
             * 若 SDK 暂不支持的类型，请使用 Map.class，嵌套的 Json 对象将被转换成 LinkedTreeMap
             */
            transaction = notificationParser.parse(requestParam, Transaction.class);

        } catch (Exception e) {
            log.error("[微信支付通知回调] 验签、解密失败:{}", e.getMessage(), e);
            return WechatPayNotifyVo.fail();
        }
        log.info("[微信支付通知回调] 当前支付状态：{} ,transaction： {}", transaction.getTradeState(), transaction);

        String notifyId = bodyMap.get("id").toString();

        String outTradeNo = transaction.getOutTradeNo();
        String tradeNo = transaction.getTransactionId();
        LocalDateTime paymentAt = LocalDateUtils.toLocalDateTime(transaction.getSuccessTime());
        String callbackContent = jsonUtils.objectToJson(transaction);
        Payments payments = paymentsService.getPaymentByOutTradeNo(outTradeNo);

        BigDecimal totalAmount = AmountUtils.toBigDecimal(transaction.getAmount().getTotal());
        if (payments.getTotalAmount().compareTo(totalAmount) != 0) {
            log.warn("微信金额不匹配, outTradeNo={}, db={}, cb={}",
                    outTradeNo, payments.getTotalAmount(), totalAmount);
            return WechatPayNotifyVo.fail();
        }

        // 已经处理过的支付信息
        if (!PaymentStatusEnum.UNPAID.equals(payments.getPaymentStatus())) {
            return WechatPayNotifyVo.ok();
        }

        // 5) 添加标识
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(notifyId, notifyId, 1461, TimeUnit.MINUTES);
        if (Boolean.TRUE.equals(flag)) {
            payments.setTradeNo(tradeNo);
            payments.setPaymentAt(paymentAt);
            payments.setPaymentStatus(PaymentStatusEnum.PAID);
            payments.setCallbackAt(callbackAt);
            payments.setCallbackContent(callbackContent);
            int cnt = paymentsService.updatePayment(payments);
            if (cnt > 0) {
                // 发送消息到订单服务
                PaymentSuccessMessage message = PaymentSuccessMessage.builder()
                        .orderNo(payments.getOrderNo())
                        .tradeNo(tradeNo)
                        .outTradeNo(outTradeNo)
                        .paymentAt(paymentAt)
                        .totalAmount(totalAmount)
                        .paymentType(PaymentTypeEnum.WECHAT_PAY)
                        .build();

                mqService.send(
                        PaymentMQConstants.EXCHANGE_TOPIC_PAYMENT_SUCCESS,
                        PaymentMQConstants.ROUTING_PAYMENT_SUCCESS,
                        message);
                return WechatPayNotifyVo.ok();
            } else {
                return WechatPayNotifyVo.fail();
            }
        }
        return WechatPayNotifyVo.fail();
    }

    /**
     * 使用回调通知请求的数据，构建 RequestParam。
     *
     * @param request HttpServletRequest
     * @param notify  notify
     * @return notify
     */
    private RequestParam buildByHeaderRequestParam(HttpServletRequest request, String notify) {
        // HTTP 头 Wechatpay-Timestamp。签名中的时间戳。
        String timestamp = request.getHeader(WECHAT_PAY_TIMESTAMP);
        // HTTP 头 Wechatpay-Nonce。签名中的随机数。
        String nonce = request.getHeader(WECHAT_PAY_NONCE);
        // HTTP 头 Wechatpay-Signature-Type。签名类型。
        String signType = request.getHeader("Wechatpay-Signature-Type");
        //HTTP 头 Wechatpay-Serial。微信支付平台证书的序列号，验签必须使用序列号对应的微信支付平台证书。
        String serialNo = request.getHeader(WECHAT_PAY_SERIAL);
        //  HTTP 头 Wechatpay-Signature。应答的微信支付签名。
        String signature = request.getHeader(WECHAT_PAY_SIGNATURE);
        // 若未设置signType，默认值为 WECHATPAY2-SHA256-RSA2048
        return new RequestParam.Builder()
                .serialNumber(serialNo)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .signType(signType)
                .body(notify)
                .build();
    }


    /**
     * 查询指定订单号的微信支付状态。
     *
     * @param outTradeNo 商家订单号，用于查询该订单的支付状态
     * @return GetPaymentStatusVo 包含支付状态信息的对象。如果支付成功、关闭或完成，则分别设置相应的支付状态；如果遇到未知支付状态，则抛出异常。
     * @throws GloboxApplicationException 当请求微信支付失败或者支付状态未知时抛出此异常。
     */
    @Override
    public GetPaymentStatusResultVo getPaymentStatus(String outTradeNo) {
        Payments payments = paymentsService.getPaymentByOutTradeNo(outTradeNo);
        Transaction transaction;
        if (ClientType.APP.equals(payments.getClientType())) {
            transaction = wechatPayAppService.getPaymentStatus(payments);
        } else if (ClientType.JSAPI.equals(payments.getClientType())) {
            transaction = wechatPayJsapiService.getPaymentStatus(payments);
        } else if (ClientType.THIRD_PARTY_JSAPI.equals(payments.getClientType())) {
            transaction = wechatPayMoonCourtJsapiService.getPaymentStatus(payments);
        } else {
            throw new GloboxApplicationException(PaymentsCode.NOT_SUPPORTED_PAYMENT_CLIENT_TYPE);
        }
        TradeStateEnum tradeState = transaction.getTradeState();

        GetPaymentStatusResultVo statusVo = GetPaymentStatusResultVo.builder()
                .outTradeNo(outTradeNo)
                .build();
        if (TradeStateEnum.SUCCESS.equals(tradeState)) {
            statusVo.setPaymentStatus(PaymentStatusEnum.PAID);
        } else if (TradeStateEnum.CLOSED.equals(tradeState)) {
            statusVo.setPaymentStatus(PaymentStatusEnum.CLOSED);
        } else if (TradeStateEnum.NOTPAY.equals(tradeState)) {
            statusVo.setPaymentStatus(PaymentStatusEnum.UNPAID);
        } else {
            log.error("未知的支付状态:{}", tradeState);
            throw new GloboxApplicationException(PaymentsCode.PAYMENT_STATUS_UNKNOW);
        }

        return statusVo;
    }


    @Override
    public ResultCode refund(Payments payments, BigDecimal refundAmount, String refundReason) {
        CreateRequest request = new CreateRequest();
        AmountReq amount = new AmountReq();
        amount.setTotal(AmountUtils.toLong(payments.getTotalAmount()));
        amount.setRefund(AmountUtils.toLong(refundAmount));
        amount.setCurrency("CNY");
        request.setAmount(amount);
        request.setOutTradeNo(payments.getOutTradeNo());
        request.setOutRefundNo(payments.getOutRequestNo());
        request.setReason(refundReason);
        Refund refund;
        try {
            refund = refundService.create(request);
        } catch (Exception e) {
            log.error("请求微信退款失败：{}", e.getMessage(), e);
            return PaymentsCode.PAYMENT_WECHAT_PAY_FAILED;
        }

        if (refund.getStatus().equals(Status.ABNORMAL)) {
            /* TODO
            描述：
                退款异常，退款到银行发现用户的卡作废或者冻结了，导致原路退款银行卡失败
            解决方案：
                1. 可前往商户平台-交易中心，手动处理此笔退款
                2. 通过发起异常退款接口进行处理（用户需要输入新的银行卡号）
             */
            return ApplicationCode.SUCCESS;
        } else if (refund.getStatus().equals(Status.CLOSED)
                || refund.getStatus().equals(Status.PROCESSING)
                || refund.getStatus().equals(Status.SUCCESS)) {
            return ApplicationCode.SUCCESS;
        } else {
            log.error("微信退款失败，未知的状态：{}, refund :{}", refund.getStatus(), jsonUtils.objectToJson(refund));
            return PaymentsCode.PAYMENT_WECHAT_PAY_FAILED;
        }
    }


    /**
     * 取消指定的支付(未支付)。
     *
     * @param payments 包含支付信息的对象，如订单编号、对外业务编号等
     */
    @Override
    public void cancel(Payments payments) {
        if (ClientType.APP.equals(payments.getClientType())) {
            wechatPayAppService.cancel(payments);
        } else if (ClientType.JSAPI.equals(payments.getClientType())) {
            wechatPayJsapiService.cancel(payments);
        } else if (ClientType.THIRD_PARTY_JSAPI.equals(payments.getClientType())) {
            wechatPayMoonCourtJsapiService.cancel(payments);
        }
    }

    @Override
    public PaymentProfitSharing profitSharing(Payments payments, String outProfitSharingNo, BigDecimal profitSharingAmount) {
        log.info("[订单完成分账] 进入微信分账:{}", payments);
        String receiverOpenId;
        String receiverRealName;
        if (payments.getSellerType().equals(SellerTypeEnum.COACH)) {
            if (profitSharingProperties.getEnableCoachProfitSharing()) {
                RpcResult<CoachInfoForProfitSharing> rpc = userDubboService.getCoachInfoForProfitSharing(payments.getReceiverId());
                CoachInfoForProfitSharing coachInfo = Assert.rpcResultOk(rpc);
                receiverOpenId = coachInfo.getAccount();
                receiverRealName = coachInfo.getRealName();
                log.info("[订单完成分账] 分账到教练:{}", receiverRealName);
            } else {
                ProfitSharingProperties.CoachAccount coachAccount = profitSharingProperties.getDefaultCoachAccount();
                receiverOpenId = coachAccount.getAccount();
                receiverRealName = coachAccount.getRealName();
                log.info("[订单完成分账] 默认分账到账户:{}", receiverRealName);
            }
        } else if (SellerTypeEnum.VENUE.equals(payments.getSellerType())) {
            return null;
        } else {
            return null;
        }

        OrdersEntity ordersEntity = switch (payments.getClientType()) {
            case APP -> wechatPayAppService.profitSharing(payments, outProfitSharingNo, receiverOpenId, receiverRealName , profitSharingAmount);
            case JSAPI,THIRD_PARTY_JSAPI,MERCHANT -> {
                log.warn("小程序，三方小程序，商家 暂不支持分账:{}", payments);
                yield null;
            }
        };

        if (ObjectUtils.isEmpty(ordersEntity)) {
            return null;
        }

        //分账状态处理
        ProfitSharingStatusEnum currentStatus;
        if (OrderStatus.FINISHED.equals(ordersEntity.getState())) {
            currentStatus = ProfitSharingStatusEnum.FINISHED;
        } else if (OrderStatus.PROCESSING.equals(ordersEntity.getState())) {
            currentStatus = ProfitSharingStatusEnum.PROCESSING;
        } else {
            currentStatus = ProfitSharingStatusEnum.PENDING;
        }

        return PaymentProfitSharing.builder()
                .paymentId(payments.getId())
                .outProfitSharingNo(outProfitSharingNo)
                .outTradeNo(payments.getOutTradeNo())
                .tradeNo(payments.getTradeNo())
                .receiverId(payments.getReceiverId())
                .profitSharingNo(ordersEntity.getOrderId())
                .status(currentStatus)
                .amount(profitSharingAmount)
                .build();
    }
}
