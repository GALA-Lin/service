package com.unlimited.sports.globox.model.demo.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import com.unlimited.sports.globox.model.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单表 - 测试 seata
 *
 * @author dk
 * @since 2025/12/20 08:53
 */
@Data
@TableName(value ="t_order")
@EqualsAndHashCode(callSuper = true)
public class Order extends BaseEntity {
    private Long userId;
    private BigDecimal money;
}
