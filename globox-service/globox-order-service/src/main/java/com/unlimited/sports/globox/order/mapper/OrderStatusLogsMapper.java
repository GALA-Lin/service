package com.unlimited.sports.globox.order.mapper;

import com.unlimited.sports.globox.model.order.entity.OrderStatusLogs;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 针对表【order_status_logs(订单状态流转日志表（支持订单级/订单项级/部分退款）)】的数据库操作Mapper
*/
@Mapper
public interface OrderStatusLogsMapper extends BaseMapper<OrderStatusLogs> {

}




