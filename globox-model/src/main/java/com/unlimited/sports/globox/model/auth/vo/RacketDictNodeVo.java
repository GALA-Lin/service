package com.unlimited.sports.globox.model.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 球拍字典节点视图
 */
@Data
@Schema(description = "球拍字典节点视图")
public class RacketDictNodeVo {

    @Schema(description = "球拍字典ID", example = "1001")
    private Long racketId;

    @Schema(description = "父级ID", example = "1000")
    private Long parentId;

    @Schema(description = "层级", example = "MODEL")
    private String level;

    @Schema(description = "名称", example = "Pure Drive 98")
    private String name;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "子节点")
    private List<RacketDictNodeVo> children;
}
