package com.unlimited.sports.globox.venue.dto;

import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.venue.entity.venues.ActivityType;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 活动预览上下文
 * 保存活动验证和查询的结果，供预览和报名方法复用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityPreviewContext {

    /**
     * 活动信息
     */
    private VenueActivity activity;

    /**
     * 场馆信息
     */
    private Venue venue;

    /**
     * 场地信息
     */
    private Court court;

    /**
     * 活动类型信息
     */
    private ActivityType activityType;
}
