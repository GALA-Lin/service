package com.unlimited.sports.globox.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.auth.entity.AuthUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * AuthUserMapper
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
@Mapper
public interface AuthUserMapper extends BaseMapper<AuthUser> {
}
