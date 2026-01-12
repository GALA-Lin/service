package com.unlimited.sports.globox.model.merchant.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 场馆级时段可用性VO
 * 用于返回场馆下所有场地的时段可用性信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueSlotAvailabilityVo {

    /**
     * 场地ID
     */
    private Long courtId;

    /**
     * 场地名称
     */
    private String courtName;

    /**
     * 场地类型（如：红土、硬地、草地）
     */
    private String courtType;

    /**
     * 该场地当天的所有时段列表
     */
    private List<SlotAvailabilityVo> slots;

    /**
     * 可用时段数量
     */
    private Integer availableCount;

    /**
     * 总时段数量
     */
    private Integer totalCount;
}