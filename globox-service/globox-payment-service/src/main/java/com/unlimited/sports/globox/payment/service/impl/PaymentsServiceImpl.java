package com.unlimited.sports.globox.payment.service.impl;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import com.unlimited.sports.globox.common.enums.payment.PaymentStatusEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.message.order.UserRefundMessage;
import com.unlimited.sports.globox.common.result.PaymentsCode;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.dubbo.order.OrderForPaymentDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.PaymentGetOrderResultDto;
import com.unlimited.sports.globox.model.payment.dto.SubmitRequestDto;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import com.unlimited.sports.globox.model.payment.vo.GetPaymentStatusResultVo;
import com.unlimited.sports.globox.model.payment.vo.SubmitResultVo;
import com.unlimited.sports.globox.payment.prop.TimeoutProperties;
import com.unlimited.sports.globox.payment.service.AlipayService;
import com.unlimited.sports.globox.payment.service.PaymentsService;
import com.unlimited.sports.globox.payment.mapper.PaymentsMapper;
import com.unlimited.sports.globox.payment.service.WechatPayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 针对表【payments(支付信息表)】的数据库操作Service实现
 */
@Slf4j
@Service
public class PaymentsServiceImpl implements PaymentsService {

    @Autowired
    private TimeoutProperties timeoutProperties;

    @Autowired
    private PaymentsMapper paymentsMapper;

    @Autowired
    private JsonUtils jsonUtils;

    @DubboReference(group = "rpc")
    private OrderForPaymentDubboService orderForPaymentDubboService;

    @Lazy
    @Autowired
    private PaymentsService thisService;

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private WechatPayService wechatPayService;

    /**
     * 根据对外业务编号获取支付信息。
     *
     * @param outTradeNo 对外业务编号
     * @return 指定条件下的支付信息，如果不存在则返回null
     */
    @Override
    public Payments getPaymentByOutTradeNo(String outTradeNo) {
        return paymentsMapper.selectOne(
                Wrappers.<Payments>lambdaQuery()
                        .eq(Payments::getOutTradeNo, outTradeNo));
    }

    @Override
    public int updatePayment(Payments payments) {
        return paymentsMapper.updateById(payments);
    }


    /**
     * 提交下单
     *
     * @param dto 下单信息
     * @return orderStr / prepayId
     */
    @Override
    public SubmitResultVo submit(SubmitRequestDto dto) {
        Long orderNo = dto.getOrderNo();
        PaymentTypeEnum paymentType = PaymentTypeEnum.from(dto.getPaymentTypeCode());
        // 1) 请求订单 rpc 接口，确认订单信息
        RpcResult<PaymentGetOrderResultDto> rpcResult = orderForPaymentDubboService.paymentGetOrders(orderNo);
        Assert.rpcResultOk(rpcResult);
        PaymentGetOrderResultDto resultDto = rpcResult.getData();

        // 2) 查出所有 orderNo 的数据
        List<Payments> paymentsList = thisService.getPaymentsList(orderNo);

        if (ObjectUtils.isNotEmpty(paymentsList)) {
            // 3) 如果存在之前的支付信息时，首先判断是否存在已支付的信息
            for (Payments payments : paymentsList) {
                Assert.isTrue(payments.getPaymentStatus().equals(PaymentStatusEnum.UNPAID)
                                || payments.getPaymentStatus().equals(PaymentStatusEnum.CLOSED)
                        , PaymentsCode.ORDER_PAID);
            }
            // 取消之前所有的未支付订单，预防重复支付
            for (Payments payments : paymentsList) {
                if (payments.getPaymentStatus().equals(PaymentStatusEnum.UNPAID)) {
                    // 申请取消支付
                    if (PaymentTypeEnum.WECHAT_PAY.equals(payments.getPaymentType())) {
                        // 微信取消支付
                        wechatPayService.cancel(payments);
                    } else if (PaymentTypeEnum.ALIPAY.equals(payments.getPaymentType())) {
                        // 支付宝取消支付
                        alipayService.cancel(payments);
                    }
                    payments.setPaymentStatus(PaymentStatusEnum.CLOSED);
                    thisService.updatePayment(payments);
                }
            }
        }

        // 4) 生成新的支付订单
        String outTradeNo = UUID.randomUUID().toString().replaceAll("-", "");

        Payments payments = new Payments();
        BeanUtils.copyProperties(resultDto, payments);
        payments.setOutTradeNo(outTradeNo);
        payments.setPaymentType(paymentType);
        payments.setClientType(dto.getClientType());
        payments.setOpenId(dto.getOpenId());
        payments.setActivity(resultDto.isActivity());
        payments.setPaymentStatus(PaymentStatusEnum.UNPAID);

        int cnt = thisService.insertPayments(payments);
        Assert.isTrue(cnt > 0, PaymentsCode.PAYMENT_CREATE_FAILED);

        // 根据不同支付类型调用不同支付平台
        if (PaymentTypeEnum.ALIPAY.equals(paymentType)) {
            // 前往支付宝支付
            return alipayService.submit(payments);
        } else if (PaymentTypeEnum.WECHAT_PAY.equals(paymentType)) {
            // 前往微信支付
            return wechatPayService.submit(payments);
        } else {
            throw new GloboxApplicationException(PaymentsCode.NOT_SUPPORTED_PAYMENT_TYPE);
        }
    }


    /**
     * 查询所有 orderNo 下的 payments 数据
     *
     * @param orderNo 订单号
     * @return payments list
     */
    @Override
    public List<Payments> getPaymentsList(Long orderNo) {
        return paymentsMapper.selectList(
                Wrappers.<Payments>lambdaQuery()
                        .eq(Payments::getOrderNo, orderNo));
    }


    /**
     * 插入支付信息
     *
     * @return 影响的记录条数
     */
    @Override
    public int insertPayments(Payments payments) {
        return paymentsMapper.insert(payments);
    }


    @Override
    public String getPaymentTimeout(Payments payments) {
        int minutes = payments.isActivity() ? timeoutProperties.getActivity() : timeoutProperties.getNormal();

        if (PaymentTypeEnum.WECHAT_PAY.equals(payments.getPaymentType())) {
            return OffsetDateTime.now(ZoneOffset.ofHours(8))
                    .plusMinutes(minutes)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        }
        if (PaymentTypeEnum.ALIPAY.equals(payments.getPaymentType())) {
            return LocalDateTime.now()
                    .plusMinutes(minutes)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        throw new GloboxApplicationException(PaymentsCode.NOT_SUPPORTED_PAYMENT_TYPE);
    }


    /**
     * 获取指定订单号和对外业务编号的支付状态。
     *
     * @param outTradeNo  对外业务编号
     * @param paymentType 对外业务编号
     * @return 返回包含支付状态信息的GetPaymentStatusVo对象
     * @throws GloboxApplicationException 如果不支持的支付类型，则抛出此异常
     */
    @Override
    public GetPaymentStatusResultVo getPaymentStatus(String outTradeNo, PaymentTypeEnum paymentType) {
        if (PaymentTypeEnum.ALIPAY.equals(paymentType)) {
            return alipayService.getPaymentStatus(outTradeNo);
        } else if (PaymentTypeEnum.WECHAT_PAY.equals(paymentType)) {
            return wechatPayService.getPaymentStatus(outTradeNo);
        } else {
            throw new GloboxApplicationException(PaymentsCode.NOT_SUPPORTED_PAYMENT_TYPE);
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refund(UserRefundMessage message) {
        // 根据 orderId 查询支付信息
        Payments payments = thisService.getPaymentByOutTradeNo(message.getOutTradeNo());

        // 判断
        if (payments == null) {
            return false;
        }

        payments.setOutRequestNo(message.getOutRequestNo());

        BigDecimal refunded = payments.getRefundAmount() == null ? BigDecimal.ZERO : payments.getRefundAmount();
        BigDecimal currentRefundAmount = refunded.add(message.getRefundAmount());
        payments.setRefundAmount(currentRefundAmount);

        BigDecimal remain = payments.getTotalAmount().subtract(currentRefundAmount);

        if (remain.compareTo(BigDecimal.ZERO) == 0) {
            payments.setPaymentStatus(PaymentStatusEnum.CLOSED); // 或 REFUNDED，看你枚举语义
        } else if (remain.compareTo(BigDecimal.ZERO) > 0) {
            payments.setPaymentStatus(PaymentStatusEnum.PARTIALLY_REFUNDED);
        } else {
            log.error("申请退款的金额大于订单可退金额，payments:{}", jsonUtils.objectToJson(payments));
            return false;
        }

        // 具体支付平台实现
        if (PaymentTypeEnum.ALIPAY.equals(payments.getPaymentType())) {
            // 支付宝退款实现
            return alipayService.refund(payments, message.getRefundAmount(), message.getRefundReason());
        } else if (PaymentTypeEnum.WECHAT_PAY.equals(payments.getPaymentType())) {
            // 微信支付退款实现
            return wechatPayService.refund(payments, message.getRefundAmount(), message.getRefundReason());
        } else {
            // 不支持的退款方式
            throw new GloboxApplicationException(PaymentsCode.NOT_SUPPORTED_PAYMENT_TYPE);
        }


    }
}