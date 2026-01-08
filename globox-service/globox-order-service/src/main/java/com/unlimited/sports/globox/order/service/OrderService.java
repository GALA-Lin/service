package com.unlimited.sports.globox.order.service;

import com.unlimited.sports.globox.common.enums.order.SellerTypeEnum;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.order.dto.SellerCancelOrderResultDto;
import com.unlimited.sports.globox.dubbo.order.dto.SellerConfirmResultDto;
import com.unlimited.sports.globox.model.order.dto.*;
import com.unlimited.sports.globox.model.order.vo.CancelOrderResultVo;
import com.unlimited.sports.globox.model.order.vo.GetOrderDetailsVo;
import com.unlimited.sports.globox.model.order.vo.GetOrderVo;
import com.unlimited.sports.globox.model.order.vo.CreateOrderResultVo;

import javax.validation.Valid;

/**
 * 针对表【orders(订单主表)】的数据库操作Service
 */
public interface OrderService {


    /**
     * 创建订场订单
     *
     * @param dto 创建订单参数
     * @return 创建结果
     */
    CreateOrderResultVo createVenueOrder(@Valid CreateVenueOrderDto dto);


    /**
     * 用户创建活动订单
     *
     * @param dto 创建活动订单参数
     * @return 创建结果
     */
    CreateOrderResultVo createVenueActivityOrder(CreateVenueActivityOrderDto dto);


    /**
     * 创建教练订单
     *
     * @param dto 包含创建教练订单所需参数的数据传输对象，包括预订日期和预订的场地时段ID列表
     * @return 包含创建订单结果的信息对象，主要包含生成的订单号
     */
    CreateOrderResultVo createCoachOrder(@Valid CreateCoachOrderDto dto);


    /**
     * 获取用户的订单列表
     *
     * @return 订单列表
     */
    PaginationResult<GetOrderVo> getOrderPage(GetOrderPageDto pageDto);


    /**
     * 获取用户的订单详情
     *
     * @param dto 查询订单的参数
     * @return 订单详情
     */
    GetOrderDetailsVo getDetails(GetOrderDetailsDto dto);


    /**
     * 取消未支付的订单。
     *
     * @param orderNo 包含取消订单所需信息的数据传输对象，包括订单号和可选的取消原因
     * @return 返回包含订单号、当前订单状态、状态描述以及取消时间的结果对象
     */
    CancelOrderResultVo cancelUnpaidOrder(Long orderNo);


}
