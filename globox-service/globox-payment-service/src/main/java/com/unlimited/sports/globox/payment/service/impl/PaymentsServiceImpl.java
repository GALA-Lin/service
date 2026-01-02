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

import java.time.LocalDateTime;
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
    public Payments getPaymentInfo(String outTradeNo, PaymentTypeEnum paymentType) {
        return paymentsMapper.selectOne(
                Wrappers.<Payments>lambdaQuery()
                        .eq(Payments::getOutTradeNo, outTradeNo)
                        .eq(Payments::getPaymentType, paymentType));
    }

    @Override
    public boolean updatePaymentSuccess(Long id, Map<String, String> paramsMap) {
        String tradeNo = paramsMap.get("trade_no");
        String callbackContent = jsonUtils.objectToJson(paramsMap);
        LocalDateTime paymentAt = LocalDateUtils.from(paramsMap.get("gmt_payment"));
        LocalDateTime callbackAt = LocalDateUtils.from(paramsMap.get("notify_time"));

        int rows = paymentsMapper.updatePaidIfUnpaid(
                id,
                tradeNo,
                PaymentStatusEnum.PAID,
                PaymentStatusEnum.UNPAID,
                paymentAt,
                callbackAt,
                callbackContent);

        // rows == 1：说明本次回调第一次把 UNPAID 改成 PAID（应当发 MQ）
        // rows == 0：说明已经处理过（重复回调/并发回调），直接认为成功但不再发 MQ
        return rows == 1;
    }
}