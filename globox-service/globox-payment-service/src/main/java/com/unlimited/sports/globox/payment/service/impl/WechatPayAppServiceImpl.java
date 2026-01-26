package com.unlimited.sports.globox.payment.service.impl;

import com.google.gson.Gson;
import com.unlimited.sports.globox.common.enums.order.SellerTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaymentsCode;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import com.unlimited.sports.globox.model.payment.vo.SubmitResultVo;
import com.unlimited.sports.globox.payment.consts.ProfitSharingReceiverTypeConst;
import com.unlimited.sports.globox.payment.prop.WechatPayProperties;
import com.unlimited.sports.globox.payment.service.PaymentsService;
import com.unlimited.sports.globox.payment.service.WechatPayAppService;
import com.unlimited.sports.globox.payment.utils.AmountUtils;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.service.payments.app.AppServiceExtension;
import com.wechat.pay.java.service.payments.app.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.profitsharing.ProfitsharingService;
import com.wechat.pay.java.service.profitsharing.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * App 微信支付
 */
@Slf4j
@Service
public class WechatPayAppServiceImpl implements WechatPayAppService {

    @Autowired
    private ProfitsharingService profitsharingService;

    @Lazy
    @Autowired
    private PaymentsService paymentsService;

    @Autowired
    private WechatPayProperties wechatPayProperties;

    @Autowired
    private AppServiceExtension appService;

    @Autowired
    private JsonUtils jsonUtils;


    /**
     * 提交 APP 端支付请求到微信支付平台。
     *
     * @param payments 包含支付信息的对象，如订单编号、openID等
     * @return 返回微信支付平台返回的预支付交易会话标识，用于后续调起支付
     */
    @Override
    public SubmitResultVo submit(Payments payments) {
        PrepayRequest prepayRequest = new PrepayRequest();
        prepayRequest.setAppid(wechatPayProperties.getAppid());
        prepayRequest.setMchid(wechatPayProperties.getMchid());
        prepayRequest.setDescription(payments.getSubject());
        prepayRequest.setOutTradeNo(payments.getOutTradeNo());
        prepayRequest.setTimeExpire(paymentsService.getPaymentTimeout(payments));
        prepayRequest.setNotifyUrl(wechatPayProperties.getNotifyPaymentUrl());
        // 设置分账
        if (payments.getProfitSharing()) {
            SettleInfo settleInfo = new SettleInfo();
            settleInfo.setProfitSharing(true);
            prepayRequest.setSettleInfo(settleInfo);
        }
        Amount amount = new Amount();
        amount.setTotal(AmountUtils.toInteger(payments.getTotalAmount()));
        prepayRequest.setAmount(amount);
        PrepayWithRequestPaymentResponse response = appService.prepayWithRequestPayment(prepayRequest);
        log.info("prepay 成功：{}", jsonUtils.objectToJson(response));

        Gson gson = new Gson();
        return SubmitResultVo.builder()
                .outTradeNo(payments.getOutTradeNo())
                .orderStr(gson.toJson(response))
                .build();
    }


    /**
     * 获取指定 APP 端支付信息的支付状态。
     *
     * @param payments 包含支付详情的对象，如订单编号、对外业务编号等
     * @return 返回包含支付状态信息的Transaction对象
     */
    @Override
    public Transaction getPaymentStatus(Payments payments) {
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setMchid(wechatPayProperties.getMchid());
        request.setOutTradeNo(payments.getOutTradeNo());
        try {
            return appService.queryOrderByOutTradeNo(request);
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
        closeRequest.setMchid(wechatPayProperties.getMchid());
        closeRequest.setOutTradeNo(payments.getOutTradeNo());
        appService.closeOrder(closeRequest);
    }

    /**
     * APP 微信支付处理逻辑
     *
     * @param payments            payment info
     * @param outProfitSharingNo  提供给第三方的分账单号
     * @param receiverOpenId      接收方 open id
     * @param receiverRealName    分账接收方真实姓名
     * @param profitSharingAmount 接收方收到的分账金额
     */
    @Override
    public OrdersEntity profitSharing(Payments payments, String outProfitSharingNo, String receiverOpenId, String receiverRealName, BigDecimal profitSharingAmount) {
        CreateOrderRequest profitSharingRequest = new CreateOrderRequest();
        profitSharingRequest.setAppid(wechatPayProperties.getAppid());
        profitSharingRequest.setTransactionId(payments.getTradeNo());
        profitSharingRequest.setOutOrderNo(outProfitSharingNo);
        // true：分账后剩余金额发送到分账方可用余额（不可再次分账）
        profitSharingRequest.setUnfreezeUnsplit(true);
        CreateOrderReceiver receiver = new CreateOrderReceiver();
        if (SellerTypeEnum.COACH.equals(payments.getSellerType())) {
            receiver.setType(ProfitSharingReceiverTypeConst.COACH);
            receiver.setAccount(receiverOpenId);
            receiver.setDescription("【GLOBOX】分账百分之30到【%s】".formatted(receiverRealName));
            receiver.setAmount(AmountUtils.toLong(profitSharingAmount));
        } else {
            return null;
        }
        profitSharingRequest.setReceivers(List.of(receiver));

        // 轮询查询日志
        OrdersEntity order = profitsharingService.createOrder(profitSharingRequest);
        QueryOrderRequest queryOrderRequest = new QueryOrderRequest();
        queryOrderRequest.setOutOrderNo(order.getOutOrderNo());
        queryOrderRequest.setTransactionId(order.getTransactionId());

        while (OrderStatus.PROCESSING.equals(order.getState())) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignore) {}
            OrdersEntity ordersEntity = profitsharingService.queryOrder(queryOrderRequest);
            order.setState(ordersEntity.getState());
        }
        return order;
    }
}
