package com.unlimited.sports.globox.merchant.service;

import com.unlimited.sports.globox.model.merchant.dto.RefundRequestDto;
import com.unlimited.sports.globox.model.merchant.vo.RefundResultVo;

/**
 * @author Linsen Hu
 * @since  2025/12/22 09:51
 * 模拟退款服务接口
 * 说明：此接口由其他模块实现，当前模块只定义规范
 */
public interface RefundService {

    /**
     * 发起退款
     *
     * @param request 退款请求
     * @return 退款结果
     */
    RefundResultVo processRefund(RefundRequestDto request);

    /**
     * 查询退款状态
     *
     * @param refundNo 退款单号
     * @return 退款结果
     */
    RefundResultVo queryRefundStatus(String refundNo);
}