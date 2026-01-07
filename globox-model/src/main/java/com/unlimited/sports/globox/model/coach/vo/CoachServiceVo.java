package com.unlimited.sports.globox.model.coach.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @since 2025/12/31 17:02
 * 供用户查询教练提供的服务信息Vo
 */
@Data
@Builder
public class CoachServiceVo {
    /**
     * 服务ID
     */
    private Long serviceTypeId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 课程封面
     */
    private String courseCover;

    /**
     * 服务类型：1-一对一，2-一对二，3-小班
     */
    private Integer serviceType;

    /**
     * 服务类型描述
     */
    private String serviceTypeDesc;

    /**
     * 时长（分钟）
     */
    private Integer duration;

    /**
     * 价格（元）
     */
    private BigDecimal price;

    /**
     * 服务描述
     */
    private String description;
}
