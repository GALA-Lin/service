package com.unlimited.sports.globox.venue.adapter.dto.aitennis;

import lombok.Data;

/**
 * aitennis事件项目
 */
@Data
public class AitennisEventItem {

    /**
     * 项目ID（rent_price_id 或 lock_court_id）
     */
    private String id;

    /**
     * 类型：rent_price、lock_court
     */
    private String type;

    /**
     * 数据（价格信息或锁场信息）
     */
    private AitennisEventData data;
}
