package com.unlimited.sports.globox.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.demo.dto.DeductDTO;
import com.unlimited.sports.globox.model.demo.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * AccountMapper - 测试 seata
 *
 * @author dk
 * @since 2025/12/20 09:11
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {
    @Update("update t_account set balance = balance - #{deductDTO.money} where user_id = #{deductDTO.userId}")
    int deduct(@Param("deductDTO")DeductDTO deductDTO);
}
