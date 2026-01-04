package com.unlimited.sports.globox.payment.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import com.unlimited.sports.globox.common.enums.payment.PaymentStatusEnum;
import com.unlimited.sports.globox.common.result.PaymentsCode;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.common.utils.LocalDateUtils;
import com.unlimited.sports.globox.dubbo.order.dto.PaymentGetOrderResultDto;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import com.unlimited.sports.globox.payment.service.PaymentsService;
import com.unlimited.sports.globox.payment.mapper.PaymentsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * 针对表【payments(支付信息表)】的数据库操作Service实现
 */
@Slf4j
@Service
public class PaymentsServiceImpl implements PaymentsService {

    @Autowired
    private JsonUtils jsonUtils;

    @Autowired
    private PaymentsMapper paymentsMapper;


    /**
     * 保存支付信息
     *
     * @param orderResultDto order 相关信息
     * @param paymentType    支付类型枚举
     * @return 是否插入成功，如果 false 代表之前已存在记录
     */
    @Override
    public boolean savePayments(PaymentGetOrderResultDto orderResultDto, PaymentTypeEnum paymentType) {
        // 先查询是否存在记录
        Payments existPayments = paymentsMapper.selectOne(
                Wrappers.<Payments>lambdaQuery()
                        .eq(Payments::getOrderNo, orderResultDto.getOrderNo())
                        .eq(Payments::getPaymentType, paymentType));

        if (existPayments != null) {
            return false;
        }

        Payments payments = new Payments();
        BeanUtils.copyProperties(orderResultDto, payments);

        payments.setPaymentType(paymentType);

        int insert = paymentsMapper.insert(payments);
        Assert.isTrue(insert > 0, PaymentsCode.PAYMENT_SAVE_FAILED);
        return true;
    }


    /**
     * 获取指定对外业务编号和支付类型的支付信息。
     *
     * @param outTradeNo  对外业务编号
     * @param paymentType 支付类型枚举
     * @return 指定条件下的支付信息，如果不存在则返回null
     */
    @Override
    public Payments getPaymentInfoByType(String outTradeNo, PaymentTypeEnum paymentType) {
        return paymentsMapper.selectOne(
                Wrappers.<Payments>lambdaQuery()
                        .eq(Payments::getOutTradeNo, outTradeNo)
                        .eq(Payments::getPaymentType, paymentType));
    }


    /**
     * 根据对外业务编号获取支付信息。
     *
     * @param outTradeNo 对外业务编号
     * @return 指定条件下的支付信息，如果不存在则返回null
     */
    @Override
    public Payments getPaymentInfoByOutTradeNo(String outTradeNo) {
        return paymentsMapper.selectOne(
                Wrappers.<Payments>lambdaQuery()
                        .eq(Payments::getOutTradeNo, outTradeNo));
    }

    @Override
    public void updatePayment(Payments payments) {
        paymentsMapper.updateById(payments);
    }


    @Override
    public boolean updatePaymentSuccess(Long id, Map<String, String> paramsMap) {
        String tradeNo = paramsMap.get("trade_no");
        // 支付宝支付时间字段：gmt_payment（你的代码里已经这么用）
        LocalDateTime paymentAt= LocalDateUtils.from(paramsMap.get("gmt_payment"));
        LocalDateTime callbackAt = LocalDateUtils.from(paramsMap.get("notify_time"));
        String callbackContent = jsonUtils.objectToJson(paramsMap);

        int rows = paymentsMapper.updatePaidIfUnpaid(
                id,
                tradeNo,
                paymentAt,
                callbackAt,
                callbackContent,
                PaymentStatusEnum.PAID.getCode(),
                PaymentStatusEnum.UNPAID.getCode()
        );
        return rows == 1;
    }

    @Override
    public boolean updatePaymentClosed(Long id, Map<String, String> paramsMap) {
        LocalDateTime callbackAt = LocalDateTime.now();
        String callbackContent = jsonUtils.objectToJson(paramsMap);

        int rows = paymentsMapper.updateClosedIfUnpaid(
                id,
                callbackAt,
                callbackContent,
                PaymentStatusEnum.CLOSED.getCode(),
                PaymentStatusEnum.UNPAID.getCode()
        );
        return rows == 1;
    }

    @Override
    public void appendCallback(Long id, Map<String, String> paramsMap) {
        paymentsMapper.updateCallbackOnly(
                id,
                LocalDateTime.now(),
                jsonUtils.objectToJson(paramsMap)
        );
    }

    @Override
    public boolean updateRefundResult(Long id, String outRequestNo, boolean isFullRefund, Map<String, String> paramsMap) {
        LocalDateTime callbackAt = LocalDateTime.now();
        String callbackContent = jsonUtils.objectToJson(paramsMap);

        if (isFullRefund) {
            int rows = paymentsMapper.updateRefundedIfPaidOrPartial(
                    id,
                    outRequestNo,
                    callbackAt,
                    callbackContent,
                    PaymentStatusEnum.REFUND.getCode(),
                    PaymentStatusEnum.PAID.getCode(),
                    PaymentStatusEnum.PARTIALLY_REFUNDED.getCode()
            );
            return rows == 1;
        } else {
            int rows = paymentsMapper.updatePartiallyRefundedIfPaid(
                    id,
                    outRequestNo,
                    callbackAt,
                    callbackContent,
                    PaymentStatusEnum.PARTIALLY_REFUNDED.getCode(),
                    PaymentStatusEnum.PAID.getCode()
            );
            return rows == 1;
        }
    }


    @Override
    public void tryMarkPaidIfUnpaid(Long id, Map<String, String> paramsMap) {
        // refund 回调里也会带 gmt_payment / trade_no（你给的样例是有的）
        String tradeNo = paramsMap.get("trade_no");
        LocalDateTime paymentAt = paymentAt = LocalDateUtils.from(paramsMap.get("gmt_payment"));

        int rows = paymentsMapper.updatePaidIfUnpaid(
                id,
                tradeNo,
                paymentAt,
                LocalDateTime.now(),
                jsonUtils.objectToJson(paramsMap),
                PaymentStatusEnum.PAID.getCode(),
                PaymentStatusEnum.UNPAID.getCode()
        );

        if (rows == 1) {
            log.info("退款回调兜底：已将支付单从 UNPAID 补标记为 PAID, id={}", id);
        }
    }

    @Override
    public boolean updateRefunded(Long id, String outRequestNo, BigDecimal refundFee,
            LocalDateTime refundAt, Map<String, String> paramsMap) {

        // 全额退款：只允许 PAID / PARTIALLY_REFUNDED -> REFUND
        int rows = paymentsMapper.updateRefundedIfPaidOrPartial(
                id,
                outRequestNo,
                LocalDateTime.now(),
                jsonUtils.objectToJson(paramsMap),
                PaymentStatusEnum.REFUND.getCode(),
                PaymentStatusEnum.PAID.getCode(),
                PaymentStatusEnum.PARTIALLY_REFUNDED.getCode()
        );

        // rows==1 代表第一次进入 REFUND
        return rows == 1;
    }

    @Override
    public boolean updatePartiallyRefunded(Long id, String outRequestNo, BigDecimal refundFee,
            LocalDateTime refundAt, Map<String, String> paramsMap) {

        String callbackContent = jsonUtils.objectToJson(paramsMap);
        LocalDateTime callbackAt = LocalDateTime.now();

        // 1) 第一次部分退款：PAID -> PARTIALLY_REFUNDED
        int rows = paymentsMapper.updatePartialIfPaid(
                id,
                outRequestNo,
                callbackAt,
                callbackContent,
                PaymentStatusEnum.PARTIALLY_REFUNDED.getCode(),
                PaymentStatusEnum.PAID.getCode()
        );

        if (rows == 1) {
            return true; // 第一次部分退款成功落库
        }

        // 2) 后续部分退款：状态已是 PARTIALLY_REFUNDED，仅更新回调记录（不算 first）
        paymentsMapper.touchPartialIfPartial(
                id,
                outRequestNo,
                callbackAt,
                callbackContent,
                PaymentStatusEnum.PARTIALLY_REFUNDED.getCode()
        );

        return false;
    }
}