package com.unlimited.sports.globox.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.demo.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * order mapper - 测试 seata
 *
 * @author dk
 * @since 2025/12/20 08:56
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
