package com.unlimited.sports.globox.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.auth.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * UserProfileMapper
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
