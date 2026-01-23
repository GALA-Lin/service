package com.unlimited.sports.globox.payment.service;

import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.ResultCode;
import com.unlimited.sports.globox.dubbo.payment.dto.UserRefundRequestDto;
import com.unlimited.sports.globox.model.payment.dto.SubmitRequestDto;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import com.unlimited.sports.globox.model.payment.vo.GetPaymentStatusResultVo;
import com.unlimited.sports.globox.model.payment.vo.SubmitResultVo;

import java.util.List;

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
    SubmitResultVo submit(SubmitRequestDto dto);


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


    /**
     * 获取指定订单号和对外业务编号的支付状态。
     *
     * @param outTradeNo  对外业务编号
     * @param paymentType 对外业务编号
     * @return 返回包含支付状态信息的GetPaymentStatusVo对象
     * @throws GloboxApplicationException 如果不支持的支付类型，则抛出此异常
     */
    GetPaymentStatusResultVo getPaymentStatus(String outTradeNo, PaymentTypeEnum paymentType);


    ResultCode refundAction(UserRefundRequestDto dto);

    ResultCode refund(UserRefundRequestDto dto);
}
