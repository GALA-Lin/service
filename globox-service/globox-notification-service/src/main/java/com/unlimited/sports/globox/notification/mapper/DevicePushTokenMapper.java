package com.unlimited.sports.globox.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.notification.entity.DevicePushToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备推送Token映射表 Mapper
 */
@Mapper
public interface DevicePushTokenMapper extends BaseMapper<DevicePushToken> {

}
