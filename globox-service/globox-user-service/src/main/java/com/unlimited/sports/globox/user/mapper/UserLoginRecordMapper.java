package com.unlimited.sports.globox.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.auth.entity.UserLoginRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * UserLoginRecordMapper
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
@Mapper
public interface UserLoginRecordMapper extends BaseMapper<UserLoginRecord> {
}
