package com.unlimited.sports.globox.payment.service;

import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import com.unlimited.sports.globox.dubbo.order.dto.PaymentGetOrderResultDto;
import com.unlimited.sports.globox.model.payment.entity.Payments;

import java.util.Map;

/**
 * 针对表【payments(支付信息表)】的数据库操作Service
 */
public interface PaymentsService {

    /**
     * 保存支付信息
     *
     * @param orderResultDto order 相关信息
     * @param paymentType    支付类型枚举
     * @return 是否插入成功，如果 false 代表之前已存在记录
     */
    boolean savePayments(PaymentGetOrderResultDto orderResultDto, PaymentTypeEnum paymentType);

    /**
     * 获取指定对外业务编号和支付类型的支付信息。
     *
     * @param outTradeNo 对外业务编号
     * @param paymentType 支付类型枚举
     * @return 指定条件下的支付信息，如果不存在则返回null
     */
    Payments getPaymentInfo(String outTradeNo, PaymentTypeEnum paymentType);

    /**
     * 更新支付成功信息
     * @param id Payments id
     * @param paramsMap 回调参数
     * @return 是否已经第一次被更新
     */
    boolean updatePaymentSuccess(Long id, Map<String, String> paramsMap);
}
