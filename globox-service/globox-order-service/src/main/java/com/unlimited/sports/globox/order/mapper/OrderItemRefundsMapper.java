package com.unlimited.sports.globox.order.mapper;

import com.unlimited.sports.globox.model.order.entity.OrderItemRefunds;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 针对表【order_item_refunds(订单项退款表（部分退款的事实表）)】的数据库操作Mapper
 */
@Mapper
public interface OrderItemRefundsMapper extends BaseMapper<OrderItemRefunds> {

}




