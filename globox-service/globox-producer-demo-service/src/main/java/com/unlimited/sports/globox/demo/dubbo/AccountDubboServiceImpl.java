package com.unlimited.sports.globox.demo.dubbo;

import com.unlimited.sports.globox.common.result.DemoCode;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.demo.mapper.AccountMapper;
import com.unlimited.sports.globox.dubbo.demo.AccountDubboService;
import com.unlimited.sports.globox.model.demo.dto.DeductDTO;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

/**
 * TODO
 *
 * @author dk
 * @since 2025/12/20 09:09
 */
@Component
@DubboService(group = "rpc")
@RequiredArgsConstructor
public class AccountDubboServiceImpl implements AccountDubboService {

    private final AccountMapper accountMapper;
    @Override
    @GlobalTransactional
    public Boolean deduct(DeductDTO deductDTO) {
        int deduct = accountMapper.deduct(deductDTO);
        Assert.isTrue(deduct > 0 , DemoCode.ACCOUNT_DEDUCT_FAILED);
        return true;
    }
}
