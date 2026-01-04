package com.unlimited.sports.globox.payment.service;

import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import com.unlimited.sports.globox.common.enums.payment.PaymentStatusEnum;
import com.unlimited.sports.globox.dubbo.order.dto.PaymentGetOrderResultDto;
import com.unlimited.sports.globox.model.payment.entity.Payments;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
     * @param outTradeNo  对外业务编号
     * @param paymentType 支付类型枚举
     * @return 指定条件下的支付信息，如果不存在则返回null
     */
    Payments getPaymentInfoByType(String outTradeNo, PaymentTypeEnum paymentType);

    /**
     * 根据对外业务编号获取支付信息。
     *
     * @param outTradeNo 对外业务编号
     * @return 指定条件下的支付信息，如果不存在则返回null
     */
    Payments getPaymentInfoByOutTradeNo(String outTradeNo);

    /**
     * 交易关闭：UNPAID -> CLOSED（幂等）
     *
     * @return true 表示第一次成功更新（可以驱动取消订单）
     */
    boolean updatePaymentClosed(Long id, Map<String, String> paramsMap);

    /**
     * 仅记录回调（不改变业务状态）
     */
    void appendCallback(Long id, Map<String, String> paramsMap);

    /**
     * 退款结果落库（由退款服务调用）
     *
     * @param isFullRefund true=全额退款完成 => REFUND；false=部分退款 => PARTIALLY_REFUNDED
     * @return true 表示第一次成功更新（可发 MQ）
     */
    boolean updateRefundResult(Long id, String outRequestNo, boolean isFullRefund, Map<String, String> paramsMap);

    /**
     * 更新支付成功信息
     *
     * @param id        Payments id
     * @param paramsMap 回调参数
     * @return 是否第一次更新成功
     */
    boolean updatePaymentSuccess(Long id, Map<String, String> paramsMap);

    void updatePayment(Payments payments);


    /**
     * 退款回调可能早于支付成功回调：兜底把 UNPAID 补标记成 PAID
     * 幂等：只有 UNPAID 才会更新
     */
    void tryMarkPaidIfUnpaid(Long id, Map<String, String> paramsMap);

    /**
     * 全额退款完成：PAID / PARTIALLY_REFUNDED -> REFUND
     * @return true 表示第一次成功置为 REFUND（可发 MQ）
     */
    boolean updateRefunded(Long id, String outRequestNo, BigDecimal refundFee,
            LocalDateTime refundAt, Map<String, String> paramsMap);

    /**
     * 部分退款：PAID -> PARTIALLY_REFUNDED（第一次）；后续部分退款只更新回调信息
     * @return true 表示第一次成功置为 PARTIALLY_REFUNDED（可发 MQ）
     */
    boolean updatePartiallyRefunded(Long id, String outRequestNo, BigDecimal refundFee,
            LocalDateTime refundAt, Map<String, String> paramsMap);
}
