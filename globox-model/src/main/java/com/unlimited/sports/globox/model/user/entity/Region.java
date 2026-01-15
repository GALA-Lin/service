package com.unlimited.sports.globox.model.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.unlimited.sports.globox.model.base.BaseEntity;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 行政区划字典实体
 *
 * level 说明：
 * 1 = 省
 * 2 = 市
 * 3 = 区 / 县
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("region")
public class Region extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 行政区划 code（主键）
     * 示例：510000 / 510100 / 510104
     */
    private String code;

    /**
     * 行政区名称
     * 示例：四川省 / 成都市 / 锦江区
     */
    private String name;

    /**
     * 行政级别
     * 1 = 省
     * 2 = 市
     * 3 = 区 / 县
     */
    private Integer level;

    /**
     * 父级行政区 code
     * 省：NULL
     * 市：省 code
     * 区：市 code
     */
    private String parentCode;

    /**
     * 是否启用
     * 1 = 启用
     * 0 = 禁用
     */
    private Integer enabled;

    /**
     * 排序号（同级排序）
     */
    private Integer sortNo;

}
