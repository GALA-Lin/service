package com.unlimited.sports.globox.model.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户实体类 - 测试 seata
 *
 * @author dk
 * @since 2025/12/20 09:10
 */
@Data
@TableName("t_account")
public class Account {
    @TableId(type = IdType.AUTO)
    private Long userId;
    private BigDecimal balance;
}
