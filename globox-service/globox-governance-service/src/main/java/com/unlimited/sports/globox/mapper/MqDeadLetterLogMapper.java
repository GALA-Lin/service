package com.unlimited.sports.globox.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.governance.entity.MQDeadLetterLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MqDeadLetterLogMapper extends BaseMapper<MQDeadLetterLog> {
}