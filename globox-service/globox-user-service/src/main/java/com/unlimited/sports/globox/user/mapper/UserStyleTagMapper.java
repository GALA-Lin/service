package com.unlimited.sports.globox.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.auth.entity.UserStyleTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户球风标签关联 Mapper
 */
@Mapper
public interface UserStyleTagMapper extends BaseMapper<UserStyleTag> {
}
