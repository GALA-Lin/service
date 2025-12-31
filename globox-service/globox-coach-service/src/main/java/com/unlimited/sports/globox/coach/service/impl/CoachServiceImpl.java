package com.unlimited.sports.globox.coach.service.impl;

import com.unlimited.sports.globox.coach.service.ICoachService;
import com.unlimited.sports.globox.model.coach.dto.QueryCoachListDto;

/**
 * @since 2025/12/31 13:58
 *
 */

public class CoachServiceImpl implements ICoachService {
    /**
     * 查询教练列表（支持筛选和排序）
     *
     * @param dto 查询条件
     * @return 分页后的教练列表
     */
    @Override
    public PaginationResult<CoachListItemVo> queryCoachList(QueryCoachListDto dto) {
        return null;
    }

    /**
     * 获取教练详情
     *
     * @param coachUserId 教练ID（用户ID）
     * @param latitude    用户纬度（可选，用于计算距离）
     * @param longitude   用户经度（可选，用于计算距离）
     * @return 教练详情
     */
    @Override
    public CoachDetailVo getCoachDetail(Long coachUserId, Double latitude, Double longitude) {
        return null;
    }
}
