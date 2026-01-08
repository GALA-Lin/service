package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unlimited.sports.globox.model.venue.entity.venues.ActivityType;
import com.unlimited.sports.globox.venue.mapper.ActivityTypeMapper;
import com.unlimited.sports.globox.venue.service.IActivityTypeService;
import org.springframework.stereotype.Service;

/**
 * 活动类型 Service 实现
 */
@Service
public class ActivityTypeServiceImpl extends ServiceImpl<ActivityTypeMapper, ActivityType>
        implements IActivityTypeService {


}
