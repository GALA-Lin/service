package com.unlimited.sports.globox.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.auth.entity.AuthIdentity;
import org.apache.ibatis.annotations.Mapper;

/**
 * AuthIdentityMapper
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
@Mapper
public interface AuthIdentityMapper extends BaseMapper<AuthIdentity> {
}
