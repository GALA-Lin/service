package com.unlimited.sports.globox.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.auth.entity.UserRacket;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户球拍关联 Mapper
 */
@Mapper
public interface UserRacketMapper extends BaseMapper<UserRacket> {
}
