package com.unlimited.sports.globox.order.mapper;

import com.unlimited.sports.globox.model.order.entity.Orders;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author beanmak1r
* @description 针对表【orders(订单主表)】的数据库操作Mapper
* @createDate 2025-12-22 09:38:15
* @Entity com.unlimited.sports.globox.model.order.entity.Orders
*/
@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {

}




