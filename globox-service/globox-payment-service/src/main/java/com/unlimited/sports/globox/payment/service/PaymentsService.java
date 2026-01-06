package com.unlimited.sports.globox.payment.service;

import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import com.unlimited.sports.globox.dubbo.order.dto.PaymentGetOrderResultDto;
import com.unlimited.sports.globox.model.payment.dto.SubmitRequestDto;
import com.unlimited.sports.globox.model.payment.entity.Payments;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 针对表【payments(支付信息表)】的数据库操作Service
 */
public interface PaymentsService {

    /**
     * 根据对外业务编号获取支付信息。
     *
     * @param outTradeNo 对外业务编号
     * @return 指定条件下的支付信息，如果不存在则返回null
     */
    Payments getPaymentByOutTradeNo(String outTradeNo);


    /**
     * 更新 payments
     *
     * @param payments 待更新的信息
     * @return 更新的条数
     */
    int updatePayment(Payments payments);


    /**
     * 提交下单
     *
     * @param dto 下单信息
     * @return orderStr / prepayId
     */
    String submit(SubmitRequestDto dto);


    /**
     * 查询所有 orderNo 下的 payments 数据
     *
     * @param orderNo 订单号
     * @return payments list
     */
    List<Payments> getPaymentsList(Long orderNo);


    /**
     * 插入支付信息
     *
     * @return 影响的记录条数
     */
    int insertPayments(Payments payments);


    String getPaymentTimeout(Payments payments);

}
