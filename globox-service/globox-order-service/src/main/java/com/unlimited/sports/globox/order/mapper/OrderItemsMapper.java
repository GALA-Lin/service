package com.unlimited.sports.globox.order.mapper;

import com.unlimited.sports.globox.model.order.entity.OrderItems;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author beanmak1r
* @description 针对表【order_items(订单明细表)】的数据库操作Mapper
* @createDate 2025-12-22 09:38:15
* @Entity com.unlimited.sports.globox.model.order.entity.OrderItems
*/
@Mapper
public interface OrderItemsMapper extends BaseMapper<OrderItems> {

}




