package com.unlimited.sports.globox.dubbo.order;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.order.dto.CoachGetOrderPageRequestDto;
import com.unlimited.sports.globox.dubbo.order.dto.CoachGetOrderResultDto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 订单服务对教练服务 提供订单相关接口
 */

public interface OrderForCoachDubboService {

    /**
     * 获取教练的订单分页信息。
     *
     * @param dto 请求参数，包含教练ID、页码和每页大小
     * @return 返回一个RpcResult对象，其中包含了分页后的订单信息列表。每个订单信息包括订单号、用户ID、场馆信息、价格明细、支付状态、订单状态等
     */
    RpcResult<IPage<CoachGetOrderResultDto>> getOrderPage(
            @Valid @NotNull(message = "请求参数不能为空")
            CoachGetOrderPageRequestDto dto);
}
