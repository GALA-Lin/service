package com.unlimited.sports.globox.order.service;

import com.unlimited.sports.globox.model.order.dto.ApplyRefundRequestDto;
import com.unlimited.sports.globox.model.order.dto.CancelRefundApplyRequestDto;
import com.unlimited.sports.globox.model.order.dto.GetRefundProgressRequestDto;
import com.unlimited.sports.globox.model.order.vo.ApplyRefundResultVo;
import com.unlimited.sports.globox.model.order.vo.CancelRefundApplyResultVo;
import com.unlimited.sports.globox.model.order.vo.GetRefundProgressVo;

import javax.validation.Valid;

/**
 * 提供订单退款相关的服务操作。
 */
public interface OrderRefundService {

    /**
     * 申请订单退款。
     *
     * @param dto 退款请求参数
     * @return 包含退款申请结果的统一响应对象
     */
    ApplyRefundResultVo applyRefund(ApplyRefundRequestDto dto);


    /**
     * 查询指定订单或退款申请的退款进度。
     *
     * @param dto 包含退款查询参数的对象，包括订单号和/或退款申请ID以及是否包含时间线
     * @return 包含退款进度详情的对象，包括订单状态、申请单状态、处理金额等信息
     */
    GetRefundProgressVo getRefundProgress(GetRefundProgressRequestDto dto);


    /**
     * 取消用户的退款申请。
     *
     * @param dto 包含取消退款请求参数的对象，包括订单号、退款申请ID以及可选的取消原因
     * @return 包含取消退款结果的信息对象，包括订单号、退款申请ID、申请单状态、订单状态、取消时间等信息
     */
    CancelRefundApplyResultVo cancelRefundApply(CancelRefundApplyRequestDto dto);
}
