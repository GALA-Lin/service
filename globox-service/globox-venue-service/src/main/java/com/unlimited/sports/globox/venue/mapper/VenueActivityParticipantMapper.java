package com.unlimited.sports.globox.venue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivityParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 活动参与者 Mapper
 */
@Mapper
public interface VenueActivityParticipantMapper extends BaseMapper<VenueActivityParticipant> {

    /**
     * 批量查询用户的参与活动次数
     *
     * @param userIds 用户ID列表
     * @return key为userId，value为count的Map列表
     */
    List<Map<String, Object>> selectUserParticipationCount(@Param("userIds") List<Long> userIds);
}
