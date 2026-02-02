package com.unlimited.sports.globox.payment.service.impl;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.enums.ThirdPartyJsapiEnum;
import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import com.unlimited.sports.globox.common.enums.payment.PaymentStatusEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaymentsCode;
import com.unlimited.sports.globox.common.result.ResultCode;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.dubbo.order.OrderForPaymentDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.PaymentGetOrderResultDto;
import com.unlimited.sports.globox.dubbo.payment.dto.UserRefundRequestDto;
import com.unlimited.sports.globox.model.payment.dto.SubmitRequestDto;
import com.unlimited.sports.globox.model.payment.entity.PaymentProfitSharing;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import com.unlimited.sports.globox.model.payment.vo.GetPaymentStatusResultVo;
import com.unlimited.sports.globox.model.payment.vo.SubmitResultVo;
import com.unlimited.sports.globox.payment.mapper.PaymentProfitSharingMapper;
import com.unlimited.sports.globox.payment.prop.TimeoutProperties;
import com.unlimited.sports.globox.payment.service.AlipayService;
import com.unlimited.sports.globox.payment.service.PaymentsService;
import com.unlimited.sports.globox.payment.mapper.PaymentsMapper;
import com.unlimited.sports.globox.payment.service.WechatPayMoonCourtJsapiService;
import com.unlimited.sports.globox.payment.service.WechatPayService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

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
    private PaymentProfitSharingMapper profitSharingMapper;

    @Autowired
    private JsonUtils jsonUtils;

    @DubboReference(group = "rpc")
    private OrderForPaymentDubboService orderForPaymentDubboService;

    @Lazy
    @Autowired
    private PaymentsService thisService;

    @Autowired
    private AlipayService alipayService;

    @Autowired(required = false)
    private WechatPayService wechatPayService;

    @Autowired(required = false)
    private WechatPayMoonCourtJsapiService wechatPayMoonCourtJsapiService;

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
     * @return orderStr / prepayInfo
     */
    @Override
    public SubmitResultVo submit(SubmitRequestDto dto) {
        Long orderNo = dto.getOrderNo();
        PaymentTypeEnum paymentType = PaymentTypeEnum.from(dto.getPaymentTypeCode());
        // 1) 请求订单 rpc 接口，确认订单信息
        RpcResult<PaymentGetOrderResultDto> rpcResult = orderForPaymentDubboService.paymentGetOrders(orderNo);
        PaymentGetOrderResultDto resultDto = Assert.rpcResultOk(rpcResult);

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
        payments.setReceiverId(resultDto.getSellerId());
        payments.setProfitSharing(resultDto.isProfitSharing());
        payments.setPaymentStatus(PaymentStatusEnum.UNPAID);
        payments.setSellerType(resultDto.getSellerType());
        payments.setReceiverId(resultDto.getSellerId());

        // 设置三方小程序
        if (dto.getClientType().equals(ClientType.THIRD_PARTY_JSAPI)) {
            // 揽月
            payments.setThirdPartyJsapi(ThirdPartyJsapiEnum.MOON_COURT);
        } else {
            payments.setThirdPartyJsapi(null);
        }

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


    /**
     * 获取超时时间
     *
     * @param payments 支付信息
     * @return 支付超时时间
     */
    @Override
    public String getPaymentTimeout(Payments payments) {
        int minutes = payments.getActivity() ? timeoutProperties.getActivity() : timeoutProperties.getNormal();

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

    /**
     * 用户退款
     */
    @Override
    @GlobalTransactional
    @Transactional(rollbackFor = Exception.class)
    public ResultCode refund(UserRefundRequestDto dto) {
        return thisService.refundAction(dto);
    }


    @Override
    public ResultCode refundAction(UserRefundRequestDto dto) {
        // 根据 orderId 查询支付信息
        Payments payments = thisService.getPaymentByOutTradeNo(dto.getOutTradeNo());

        // 判断
        if (payments == null) {
            return PaymentsCode.PAYMENT_INFO_NOT_EXIST;
        }

        payments.setOutRequestNo(dto.getOutRequestNo());

        BigDecimal refunded = payments.getRefundAmount() == null ? BigDecimal.ZERO : payments.getRefundAmount();
        BigDecimal currentRefundAmount = refunded.add(dto.getRefundAmount());
        payments.setRefundAmount(currentRefundAmount);

        BigDecimal remain = payments.getTotalAmount().subtract(currentRefundAmount);

        if (remain.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("申请退款的金额大于订单可退金额，payments:{}", jsonUtils.objectToJson(payments));
            return PaymentsCode.PAYMENT_REFUND_AMOUNT_ERROR;

        }

        // 是否整单退款
        payments.setPaymentStatus(dto.isFullRefund() ? PaymentStatusEnum.CLOSED : PaymentStatusEnum.PARTIALLY_REFUNDED);

        ResultCode resultCode;
        // 具体支付平台实现
        if (PaymentTypeEnum.ALIPAY.equals(payments.getPaymentType())) {
            // 支付宝退款实现
            resultCode = alipayService.refund(payments, dto.getRefundAmount(), dto.getRefundReason());
        } else if (PaymentTypeEnum.WECHAT_PAY.equals(payments.getPaymentType())) {
            if (payments.getThirdPartyJsapi() != null) {
                // 三方小程序退款
                if (payments.getThirdPartyJsapi().equals(ThirdPartyJsapiEnum.MOON_COURT)) {
                    resultCode = wechatPayMoonCourtJsapiService.refund(payments,
                            dto.getRefundAmount(),
                            dto.getRefundReason());
                } else {
                    resultCode = PaymentsCode.THIRD_PARTY_TYPE_NOT_EXIST;
                }
            } else {
                // 微信支付退款实现
                resultCode = wechatPayService.refund(payments, dto.getRefundAmount(), dto.getRefundReason());
            }
        } else {
            // 不支持的退款方式
            resultCode = PaymentsCode.NOT_SUPPORTED_PAYMENT_TYPE;
        }

        thisService.updatePayment(payments);
        return resultCode;
    }


    @Override
    public void profitSharing(Payments payments) {
        String outProfitSharingNo = UUID.randomUUID().toString().replaceAll("-", "");

        // 计算出分账金额
        BigDecimal totalAmount = payments.getTotalAmount();
        BigDecimal profitSharingAmount = totalAmount
                .multiply(new BigDecimal("0.30"))
                .setScale(2, RoundingMode.DOWN);

        log.info("分账金额：{} , 分账支付信息:{}", profitSharingAmount, payments);

        PaymentProfitSharing profitSharing = switch (payments.getPaymentType()) {
            case ALIPAY -> null;
            case WECHAT_PAY -> wechatPayService.profitSharing(payments, outProfitSharingNo, profitSharingAmount);
            case NONE -> null;
        };

        // 如果分账信息为空，说明当前支付方式不支持分账
        if (profitSharing == null) {
            return;
        }
        profitSharing.setAmount(profitSharingAmount);

        profitSharingMapper.insert(profitSharing);
    }


}