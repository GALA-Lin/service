package com.unlimited.sports.globox.demo.service.impl;

import com.unlimited.sports.globox.common.result.DemoCode;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.demo.mapper.OrderMapper;
import com.unlimited.sports.globox.demo.service.OrderService;
import com.unlimited.sports.globox.dubbo.demo.AccountDubboService;
import com.unlimited.sports.globox.model.demo.dto.DeductDTO;
import com.unlimited.sports.globox.model.demo.entity.Order;
import com.unlimited.sports.globox.model.demo.vo.CreateVenueOrderRequestVO;
import io.seata.spring.annotation.GlobalTransactional;
import io.seata.tm.api.transaction.Propagation;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * 订单服务 - 测试 seata
 *
 * @author dk
 * @since 2025/12/20 08:52
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;

    @DubboReference(group = "rpc")
    private AccountDubboService accountDubboService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @GlobalTransactional(
            // 当前全局事务的名称
            name = "demo-create-order",
            // 回滚异常
            rollbackFor = Exception.class,
            // 全局锁重试间隔
            lockRetryInterval = 5000,
            // 全局锁重试次数
            lockRetryTimes = 5,
            // 超时时间
            timeoutMills = 30000,
            //事务传播
            propagation = Propagation.REQUIRES_NEW
    )
    public String create(CreateVenueOrderRequestVO createVenueOrderRequestVO) {
        Order order = new Order();
        BeanUtils.copyProperties(createVenueOrderRequestVO, order);
        int cnt = orderMapper.insert(order);
        Assert.isTrue(cnt > 0, DemoCode.ORDER_CREATE_FAILED);

        DeductDTO deductDTO = new DeductDTO();
        BeanUtils.copyProperties(createVenueOrderRequestVO, deductDTO);

        boolean deduct = accountDubboService.deduct(deductDTO);
        return deduct ? "success" : "fail";
    }
}
