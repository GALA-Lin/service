package com.unlimited.sports.globox.merchant.service.impl;

import com.unlimited.sports.globox.merchant.service.RefundService;
import com.unlimited.sports.globox.model.merchant.dto.RefundRequestDto;
import com.unlimited.sports.globox.model.merchant.vo.RefundResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 退款服务默认实现（Mock）
 *
 * 说明：
 * 1. 使用 @ConditionalOnMissingBean 注解
 * 2. 当没有真实的 RefundService 实现时，使用此 Mock 实现
 * 3. 后期接入真实退款服务后，此类会自动失效
 */
@Slf4j
@Service
@ConditionalOnMissingBean(name = "realRefundServiceImpl") //
public class MockRefundServiceImpl implements RefundService {

    @Override
    public RefundResultVo processRefund(RefundRequestDto request) {
        log.warn("【退款服务未接入】使用 Mock 实现，订单号：{}，退款金额：{}",
                request.getOrderNo(), request.getRefundAmount());

        // TODO: 此处仅做日志记录，不实际发起退款
        log.info("退款请求详情：{}", request);

        // 返回模拟的成功结果
        String mockRefundNo = "REFUND_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return RefundResultVo.success(mockRefundNo, request.getRefundAmount());
    }

    @Override
    public RefundResultVo queryRefundStatus(String refundNo) {
        log.warn("【退款服务未接入】查询退款状态，退款单号：{}", refundNo);

        return RefundResultVo.builder()
                .success(true)
                .refundNo(refundNo)
                .refundStatus(2) // 模拟成功
                .build();
    }
}
