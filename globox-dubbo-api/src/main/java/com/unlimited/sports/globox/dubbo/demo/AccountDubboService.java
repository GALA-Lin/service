package com.unlimited.sports.globox.dubbo.demo;

import com.unlimited.sports.globox.model.demo.dto.DeductDTO;

/**
 * producer demo 模块 - dubbo 接口 - 测试 seata
 *
 * @author dk
 * @since 2025/12/20 09:02
 */
public interface AccountDubboService {
    Boolean deduct(DeductDTO deductDTO);
}
