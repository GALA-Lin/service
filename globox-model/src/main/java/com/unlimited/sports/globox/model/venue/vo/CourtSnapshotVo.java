package com.unlimited.sports.globox.model.venue.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * 场地信息快照
 * 公共VO，用于订单预览和订单详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourtSnapshotVo {

    /**
     * 场地ID
     */
    @NotNull
    private Long id;

    /**
     * 场地名称
     */
    @NotNull
    private String name;

    /**
     * 地面类型
     */
    @NotNull
    private Integer groundType;

    /**
     * 场地类型
     */
    @NotNull
    private Integer courtType;
}
